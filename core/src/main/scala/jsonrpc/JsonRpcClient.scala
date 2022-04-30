package jsonrpc

import cats.MonadError
import cats.implicits._

trait JsonRpcClient[F[_]] {
  def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[Either[JsonRpcClientError, B]]
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

case class ServerRespondedWithNon200CodeError(statusCode: Int) extends JsonRpcClientError

case class ServerResponseParseError() extends JsonRpcClientError

case class ServerInvalidResponseError() extends JsonRpcClientError

case class ServerRespondedWithNoResultError() extends JsonRpcClientError

case class ServerRespondWithError(error: JsonRpcError) extends JsonRpcClientError

case class JsonRpcExecuteException(error: JsonRpcClientError, method: String) extends RuntimeException(s"error while executing $method, error: ${error}")
