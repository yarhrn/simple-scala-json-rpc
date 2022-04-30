package jsonrpc

import cats.Functor
import cats.implicits._
import play.api.libs.json.JsValue

trait Handler[F[_]] {
  def handle(a: JsValue): F[Either[JsonRpcError, JsValue]]

  def methodName: String
}


object Handler {
  def create[A, B, F[_] : Functor](definition: MethodDefinition[A, B],
                                   method: A => F[Either[JsonRpcError, B]]): Handler[F] = new Handler[F] {

    override def handle(a: JsValue): F[Either[JsonRpcError, JsValue]] = {
      val request = definition.req.reads(a).get // todo fix
      method(request).map(_.map(definition.res.writes))
    }

    override def methodName: String = definition.methodName
  }
}
