package jsonrpc.client

import cats.{Functor, MonadError}
import cats.implicits._
import jsonrpc.client.JsonRpcClientError.{
  ServerInvalidResponseError,
  ServerRespondWithError,
  ServerRespondedWithEmptyBodyError,
  ServerRespondedWithNoResultError,
  ServerRespondedWithNon200CodeError,
  ServerResponseParseError,
  ServerResultMappingError
}
import jsonrpc.{JsonRpcError, JsonRpcRequest, JsonRpcResponse, MethodDefinition}
import play.api.libs.json.Json

import java.util.UUID
import scala.util.Try

trait JsonRpcClient[F[_]] {
  def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[Either[JsonRpcClientError, B]]
}

object JsonRpcClient {
  def from[F[_]: Functor](transport: TransportLayerClient[F]): JsonRpcClient[F] = new JsonRpcClient[F] {
    override def execute[A, B](
        methodDefinition: MethodDefinition[A, B],
        request: A): F[Either[JsonRpcClientError, B]] = {
      val jsonRpcRequest = JsonRpcRequest(
        jsonrpc = "2.0",
        method = methodDefinition.methodName,
        id = UUID.randomUUID().toString,
        params = methodDefinition.req.writes(request)
      )
      val requestBody = Json.stringify(Json.toJson(jsonRpcRequest))
      transport.execute(requestBody).map { response =>
        val methodName = methodDefinition.methodName
        if (response.status == 200) {
          for {
            body <- response.body.toRight(ServerRespondedWithEmptyBodyError(methodName, response.status, requestBody))
            json <- Try(Json.parse(body))
              .toEither
              .left
              .map(e => ServerResponseParseError(methodName, response.status, response.body, requestBody, e.getMessage))
            jsonRpcResponse <- Try(json.as[JsonRpcResponse])
              .toEither
              .left
              .map(e => ServerInvalidResponseError(methodName, response.status, response.body, requestBody, e.getMessage))
            _ <- jsonRpcResponse.error.map(e => ServerRespondWithError(methodName, e, requestBody)).toLeft(())
            result <- jsonRpcResponse
              .result
              .toRight(ServerRespondedWithNoResultError(methodName, response.status, response.body, requestBody))
            res <- methodDefinition
              .res
              .reads(result)
              .asEither
              .left
              .map(errors => ServerResultMappingError(methodName, response.status, response.body, requestBody, errors.toString))
          } yield res
        } else {
          Left(ServerRespondedWithNon200CodeError(methodName, response.status, response.body, requestBody))
        }
      }
    }
  }

}

trait FailingJsonRpcClient[F[_]] {
  def executeOrFail[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[B]
}

object FailingJsonRpcClient {
  def from[F[_]](client: JsonRpcClient[F])(implicit ME: MonadError[F, Throwable]): FailingJsonRpcClient[F] =
    new FailingJsonRpcClient[F] {
      override def executeOrFail[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[B] =
        client.execute(methodDefinition, request).flatMap {
          case Left(error)  => ME.raiseError(JsonRpcExecuteException(error, methodDefinition.methodName))
          case Right(value) => ME.pure(value)
        }
    }
}

sealed trait JsonRpcClientError

object JsonRpcClientError {

  private val MaxLength = 500

  private def truncate(s: String): String =
    if (s.length <= MaxLength) s else s.take(MaxLength) + s"... (truncated, total ${s.length} chars)"

  private def truncateOpt(s: Option[String]): String =
    s.map(truncate).getOrElse("<empty>")

  case class ServerRespondedWithNon200CodeError(method: String, statusCode: Int, body: Option[String], request: String)
      extends JsonRpcClientError {
    override def toString: String =
      s"Server responded with non-200 code for method '$method': status=$statusCode, body=${truncateOpt(body)}, request=${truncate(request)}"
  }

  case class ServerRespondedWithEmptyBodyError(method: String, statusCode: Int, request: String)
      extends JsonRpcClientError {
    override def toString: String =
      s"Server responded with empty body for method '$method': status=$statusCode, request=${truncate(request)}"
  }

  case class ServerResponseParseError(
      method: String,
      statusCode: Int,
      body: Option[String],
      request: String,
      parseError: String)
      extends JsonRpcClientError {
    override def toString: String =
      s"Failed to parse server response for method '$method': status=$statusCode, body=${truncateOpt(body)}, request=${truncate(request)}, error=$parseError"
  }

  case class ServerInvalidResponseError(
      method: String,
      statusCode: Int,
      body: Option[String],
      request: String,
      parseError: String)
      extends JsonRpcClientError {
    override def toString: String =
      s"Server returned invalid JSON-RPC response for method '$method': status=$statusCode, body=${truncateOpt(body)}, request=${truncate(request)}, error=$parseError"
  }

  case class ServerResultMappingError(
      method: String,
      statusCode: Int,
      body: Option[String],
      request: String,
      mappingErrors: String)
      extends JsonRpcClientError {
    override def toString: String =
      s"Failed to map result for method '$method': status=$statusCode, body=${truncateOpt(body)}, request=${truncate(request)}, errors=$mappingErrors"
  }

  case class ServerRespondedWithNoResultError(method: String, statusCode: Int, body: Option[String], request: String)
      extends JsonRpcClientError {
    override def toString: String =
      s"Server responded with no result for method '$method': status=$statusCode, body=${truncateOpt(body)}, request=${truncate(request)}"
  }

  case class ServerRespondWithError(method: String, error: JsonRpcError, request: String) extends JsonRpcClientError {
    override def toString: String =
      s"Server responded with error for method '$method': code=${error.code}, message=${error.message}, request=${truncate(request)}"
  }
}

case class JsonRpcExecuteException(error: JsonRpcClientError, method: String)
    extends RuntimeException(s"error while executing $method, error: ${error}")
