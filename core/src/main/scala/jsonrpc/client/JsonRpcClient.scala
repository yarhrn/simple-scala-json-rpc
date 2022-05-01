package jsonrpc.client

import cats.{Functor, MonadError}
import cats.implicits._
import jsonrpc.client.JsonRpcClientError.{ServerInvalidResponseError, ServerRespondWithError, ServerRespondedWithNoResultError, ServerRespondedWithNon200CodeError, ServerResponseParseError}
import jsonrpc.{JsonRpcError, JsonRpcRequest, JsonRpcResponse, MethodDefinition}
import play.api.libs.json.Json

import java.util.UUID
import scala.util.Try

trait JsonRpcClient[F[_]] {
  def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[Either[JsonRpcClientError, B]]
}

object JsonRpcClient{
  def from[F[_]: Functor](transport: TransportLayerClient[F]) = new JsonRpcClient[F] {
    override def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[Either[JsonRpcClientError, B]] = {
      val jsonRpcRequest = JsonRpcRequest(
        jsonrpc = "2.0",
        method = methodDefinition.methodName,
        id = UUID.randomUUID().toString,
        params = methodDefinition.req.writes(request)
      )
      transport.execute(Json.stringify(Json.toJson(jsonRpcRequest))).map{
        response =>
          if (response.status == 200) {
            for {
              json <- Try(Json.parse(response.body.get)).toEither.left.map(_ => ServerResponseParseError())
              jsonRpcResponse <- Try(json.as[JsonRpcResponse]).toEither.left.map(_ => ServerInvalidResponseError())
              _ <- jsonRpcResponse.error.map(ServerRespondWithError).toLeft(())
              result <- jsonRpcResponse.result.toRight(ServerRespondedWithNoResultError())
              response <- methodDefinition.res.reads(result).asEither.left.map(_ => ServerInvalidResponseError())
            } yield response
          } else {
            Left(ServerRespondedWithNon200CodeError(response.status))
          }
      }
    }
  }

}

trait FailingJsonRpcClient[F[_]] {
  def executeOrFail[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[B]
}

object FailingJsonRpcClient {
  def from[F[_]](client: JsonRpcClient[F])(implicit ME: MonadError[F, Throwable]) = new FailingJsonRpcClient[F] {
    override def executeOrFail[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[B] = client.execute(methodDefinition, request).flatMap {
      case Left(error) => ME.raiseError(JsonRpcExecuteException(error, methodDefinition.methodName))
      case Right(value) => ME.pure(value)
    }
  }
}

sealed trait JsonRpcClientError

object JsonRpcClientError {

  case class ServerRespondedWithNon200CodeError(statusCode: Int) extends JsonRpcClientError

  case class ServerResponseParseError() extends JsonRpcClientError

  case class ServerInvalidResponseError() extends JsonRpcClientError

  case class ServerRespondedWithNoResultError() extends JsonRpcClientError

  case class ServerRespondWithError(error: JsonRpcError) extends JsonRpcClientError
}

case class JsonRpcExecuteException(error: JsonRpcClientError, method: String) extends RuntimeException(s"error while executing $method, error: ${error}")
