package jsonrpc

import cats.{Functor, Monad}
import jsonrpc.client.{FailingJsonRpcClient, JsonRpcClient, JsonRpcClientError}
import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

case class MethodDefinition[Req, Res] private(methodName: String, req: Format[Req], res: Format[Res]) {


  def handler[F[_] : Monad](method: Req => F[Either[JsonRpcError, Res]]): Handler[F] = Handler.create(this, method)

  def handler[F[_] : Monad](method: F[Either[JsonRpcError, Res]])(implicit ev: Req =:= Unit): Handler[F] = Handler.create(this, (_: Req) => method)

  def execute[F[_]](request: Req)(implicit client: JsonRpcClient[F]): F[Either[JsonRpcClientError, Res]] = client.execute(this, request)

  // implicit ev: Req =!= Unit
  def execute[F[_]](implicit ev: Req =:= Unit, client: JsonRpcClient[F]) = client.execute(this, ev.flip.apply(()))

  def executeOrFail[F[_]](request: Req)(implicit client: FailingJsonRpcClient[F]): F[Res] = client.executeOrFail(this, request)

}

object MethodDefinition {
  implicit def EmtpyResponseFormat[A](implicit ev: A =:= Unit): Format[A] = Format[Unit](Reads.pure(()), Writes.apply(_ => Json.obj())).asInstanceOf[Format[A]]

  def create[A: Format, B: Format](methodName: String) = new MethodDefinition[A, B](methodName, implicitly[Format[A]], implicitly[Format[B]])
}

@annotation.implicitNotFound(msg = "Cannot prove that ${A} =!= ${B}.")
trait =!=[A, B]

object =!= {
  class Impl[A, B]

  object Impl {
    implicit def neq[A, B]: A Impl B = null

    implicit def neqAmbig1[A]: A Impl A = null

    implicit def neqAmbig2[A]: A Impl A = null
  }

  implicit def foo[A, B](implicit e: A Impl B): A =!= B = null
}