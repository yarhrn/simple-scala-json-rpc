package jsonrpc.sttp

import cats.{Functor, Monad, MonadError}
import cats.implicits._
import jsonrpc.sttp.SttpJsonRpcClient.JsonRpcSttpRequest
import jsonrpc.{JsonRpcClient, JsonRpcClientError, JsonRpcError, JsonRpcExecuteException, JsonRpcRequest, JsonRpcResponse, MethodDefinition, ServerInvalidResponseError, ServerRespondWithError, ServerRespondedWithNoResultError, ServerRespondedWithNon200CodeError, ServerResponseParseError}
import play.api.libs.json.Json
import sttp.client3.{Request, SttpBackend, basicRequest}
import sttp.model.Uri

import scala.util.Try
import java.util.UUID

object SttpJsonRpcClient {
  type JsonRpcSttpRequest = Request[Either[String, String], Any]
}

case class SttpJsonRpcClient[F[_]](sttp: SttpBackend[F, Any],
                                   adapt: JsonRpcSttpRequest => JsonRpcSttpRequest,
                                   url: String)(implicit F: Functor[F])
  extends JsonRpcClient[F] {
  override def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[Either[JsonRpcClientError, B]] = {
    val jsonRpcRequest = JsonRpcRequest(
      jsonrpc = "2.0",
      method = methodDefinition.methodName,
      id = UUID.randomUUID().toString,
      params = methodDefinition.req.writes(request)
    )

    val sttpRequest = basicRequest.post(Uri.parse(url).right.get)
      .body(Json.stringify(Json.toJson(jsonRpcRequest)))
      .header("Content-Type", "application/json")

    sttp.send(adapt(sttpRequest))
      .map {
        response =>
          if (response.code.code == 200) {
            for {
              json <- Try(Json.parse(response.body.toOption.get)).toEither.left.map(_ => ServerResponseParseError())
              jsonRpcResponse <- Try(json.as[JsonRpcResponse]).toEither.left.map(_ => ServerInvalidResponseError())
              _ <- jsonRpcResponse.error.map(ServerRespondWithError).toLeft(())
              result <- jsonRpcResponse.result.toRight(ServerRespondedWithNoResultError())
              response <- methodDefinition.res.reads(result).asEither.left.map(_ => ServerInvalidResponseError())
            } yield response
          } else {
            Left(ServerRespondedWithNon200CodeError(response.code.code))
          }
      }
  }
}

