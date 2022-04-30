package jsonrpc

import cats.Functor
import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

case class MethodDefinition[Req, Res] private(methodName: String, req: Format[Req], res: Format[Res]) {
  def handler[F[_] : Functor](method: Req => F[Either[JsonRpcError, Res]]): Handler[F] = Handler.create(this, method)
}

object MethodDefinition {
  def create[A: Format, B: Format](methodName: String) = new MethodDefinition[A, B](methodName, implicitly[Format[A]], implicitly[Format[B]])
}

trait JsonRpcEmpty

object JsonRpcEmpty {
  private val instance = new JsonRpcEmpty {}
  def apply(): JsonRpcEmpty = instance
  implicit val EmtpyResponseFormat: Format[JsonRpcEmpty] = Format(Reads.pure(JsonRpcEmpty()), Writes.apply((r: JsonRpcEmpty) => Json.obj()))
}