package jsonrpc

import cats.{Monad, MonadThrow}
import cats.implicits._
import jsonrpc.HandlerResult.HandlerResult
import play.api.libs.json._

import scala.util.Try

trait JsonRpcServer[F[_]] {
  def handle(request: String): F[String]
}

trait Handler[F[_]] {
  def handle(a: JsValue): F[Either[JsonRpcError, JsValue]]

  def methodName: String
}

case class JsonRpcRequest(jsonrpc: String, id: String, method: String, params: JsValue)

object JsonRpcRequest {
  import play.api.libs.functional.syntax._

  /** JSON-RPC ids may be either string or number. Stored as string. */
  private val idReads: Reads[String] = Reads[String] {
    case JsString(s) => JsSuccess(s)
    case JsNumber(n) => JsSuccess(n.toString)
    case other       => JsError(s"Unsupported id type: $other")
  }

  implicit val JsonRpcRequestReads: Reads[JsonRpcRequest] = (
    (__ \ "jsonrpc").read[String] and
      (__ \ "id").read[String](idReads) and
      (__ \ "method").read[String] and
      (__ \ "params").readWithDefault[JsValue](JsNull)
  )(JsonRpcRequest.apply _)

  implicit val JsonRpcRequestWrites: OWrites[JsonRpcRequest] = Json.writes[JsonRpcRequest]

  implicit val JsonRpcRequestFormat: OFormat[JsonRpcRequest] = OFormat(JsonRpcRequestReads, JsonRpcRequestWrites)
}

case class JsonRpcResponse(jsonrpc: String, id: String, result: Option[JsValue], error: Option[JsonRpcError])

object JsonRpcResponse {
  // Per JSON-RPC 2.0 exactly one of `result` / `error` is present. play-json's `Json.format`
  // omits `None` fields on write, so the absent member is simply not rendered.
  implicit val JsonRpcResponseFormat: OFormat[JsonRpcResponse] = Json.format[JsonRpcResponse]
}

object HandlerResult {
  type HandlerResult[A] = Either[JsonRpcError, A]

  def error[A](error: JsonRpcError): HandlerResult[A] = Left(error)
  def success[A](request: A): HandlerResult[A]       = Right(request)
}

object Handler {
  def create[A, B, F[_]: Monad](definition: MethodDefinition[A, B], method: A => F[HandlerResult[B]]): Handler[F] =
    new Handler[F] {
      override def handle(a: JsValue): F[HandlerResult[JsValue]] = {
        definition
          .req
          .reads(a)
          .asEither
          .left
          .map(_ => JsonRpcError.InvalidParams)
          .fold(
            error => Monad[F].pure(Left(error)),
            request => method(request).map(_.map(definition.res.writes))
          )
      }

      override def methodName: String = definition.methodName
    }
}

object JsonRpcServer {

  def create[F[_]: MonadThrow](handlers: List[Handler[F]]): JsonRpcServer[F] = new JsonRpcServer[F] {
    override def handle(request: String): F[String] = {
      val parsed = Try(Json.parse(request)).toEither.left.map(_ => JsonRpcError.ParseError)

      val res: Either[JsonRpcError, F[Either[JsonRpcError, JsValue]]] = for {
        json       <- parsed
        rpcRequest <- json.validate[JsonRpcRequest].asEither.left.map(_ => JsonRpcError.InvalidRequest(request))
        handler <- handlers
          .find(_.methodName == rpcRequest.method)
          .toRight(JsonRpcError.MethodNotFound(rpcRequest.method))
      } yield MonadThrow[F].catchNonFatal(handler.handle(rpcRequest.params)).flatten

      val id: String = parsed.toOption
        .flatMap(r => (r \ "id").asOpt[String].orElse((r \ "id").asOpt[BigDecimal].map(_.toString)))
        .getOrElse("")

      def respond(result: Option[JsValue], error: Option[JsonRpcError]): String =
        Json.stringify(Json.toJson(JsonRpcResponse(jsonrpc = "2.0", id = id, result = result, error = error)))

      res match {
        case Left(error) => MonadThrow[F].pure(respond(None, Some(error)))
        case Right(fr) =>
          fr.map {
            case Left(error)   => respond(None, Some(error))
            case Right(result) => respond(Some(result), None)
          }.handleError(e => respond(None, Some(JsonRpcError.InternalError(e))))
      }
    }
  }
}
