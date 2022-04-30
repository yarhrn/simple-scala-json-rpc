package jsonrpc.sttp

import cats.{Functor, Monad, MonadError}
import cats.implicits._
import jsonrpc.sttp.SttpJsonRpcClient.JsonRpcSttpRequest
import jsonrpc.{JsonRpcClient, JsonRpcClientError, JsonRpcError, MethodDefinition, ServerInvalidResponseError, ServerRespondedWithNon200CodeError, ServerResponseParseError}
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
    val jsonRpcRequest = Json.obj(
      "jsonrpc" -> "2.0",
      "method" -> methodDefinition.methodName,
      "id" -> UUID.randomUUID().toString,
      "request" -> methodDefinition.req.writes(request))

    val sttpRequest = basicRequest.post(Uri(url))
      .body(Json.stringify(jsonRpcRequest))
      .header("Content-Type", "application/json")

    sttp.send(adapt(sttpRequest))
      .map {
        response =>
          if (response.code.code == 200) {
            for {
              json <- Try(Json.parse(response.body.toOption.get)).toEither.left.map(_ => ServerResponseParseError())
              response <- methodDefinition.res.reads(json).asEither.left.map(_ => ServerInvalidResponseError())
            } yield response
          } else {
            Left(ServerRespondedWithNon200CodeError(response.code.code))
          }
      }
  }
}

