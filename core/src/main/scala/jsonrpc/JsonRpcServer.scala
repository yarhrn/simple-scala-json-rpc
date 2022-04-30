package jsonrpc

import cats.Functor
import play.api.libs.json.JsValue
import play.api.libs.json._
import cats.implicits._

trait JsonRpcServer[F[_]] {

  def handle(request: JsValue): F[JsValue]

}


object JsonRpcServer {
  def create[F[_] : Functor](handlers: List[Handler[F]]): JsonRpcServer[F] = new JsonRpcServer[F] {
    override def handle(request: JsValue): F[JsValue] = {
      val methodName = (request \ "method").as[String]
      val id = (request \ "id").as[String]
      val prefix = Json.obj("jsonrpc" -> "2.0", "id" -> id)
      handlers.find(_.methodName == methodName) match {
        case Some(handler) =>
          handler.handle(request).map {
            case Left(error) => prefix deepMerge Json.obj("error" -> error.render)
            case Right(success) => prefix deepMerge Json.obj("result" -> success)
          }
        case None => ??? // todo fix
      }
    }
  }
}