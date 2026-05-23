package jsonrpc

import cats.{Monad, MonadThrow}
import cats.implicits._
import jsonrpc.HandlerResult.HandlerResult
import upickle.default.{ReadWriter, macroRW, read, write, readwriter}
import upickle.jsonschema.{JsonSchema, schema}

import scala.util.Try

trait JsonRpcServer[F[_]] {
  def handle(request: String): F[String]
}

trait Handler[F[_]] {
  def handle(a: ujson.Value): F[Either[JsonRpcError, ujson.Value]]

  def methodName: String

  def introspection: MethodIntrospection
}

case class JsonRpcRequest(jsonrpc: String, id: String, method: String, params: ujson.Value)

object JsonRpcRequest {

  /** JSON-RPC ids may be either string or number. Stored as string. */
  private def parseId(v: ujson.Value): String = v match {
    case ujson.Str(s) => s
    case ujson.Num(n) => if (n == n.toLong.toDouble) n.toLong.toString else n.toString
    case other        => throw new IllegalArgumentException(s"Unsupported id type: $other")
  }

  implicit val rw: ReadWriter[JsonRpcRequest] =
    readwriter[ujson.Value].bimap[JsonRpcRequest](
      req => ujson.Obj(
        "jsonrpc" -> req.jsonrpc,
        "id"      -> req.id,
        "method"  -> req.method,
        "params"  -> req.params
      ),
      json => {
        val obj = json.obj
        JsonRpcRequest(
          jsonrpc = obj("jsonrpc").str,
          id      = parseId(obj("id")),
          method  = obj("method").str,
          params  = obj.getOrElse("params", ujson.Null)
        )
      }
    )
}

case class JsonRpcResponse(jsonrpc: String, id: String, result: Option[ujson.Value], error: Option[JsonRpcError])

object JsonRpcResponse {
  implicit val rw: ReadWriter[JsonRpcResponse] = macroRW
}

object HandlerResult {
  type HandlerResult[A] = Either[JsonRpcError, A]

  def error[A](error: JsonRpcError): HandlerResult[A] = Left(error)
  def success[A](request: A): HandlerResult[A]       = Right(request)
}

object Handler {
  def create[A, B, F[_]: Monad](definition: MethodDefinition[A, B], method: A => F[HandlerResult[B]]): Handler[F] =
    new Handler[F] {
      override def handle(a: ujson.Value): F[HandlerResult[ujson.Value]] = {
        Try(read[A](a)(using definition.reqRW)).toEither.left.map(_ => JsonRpcError.InvalidParams) match {
          case Left(error)     => Monad[F].pure(Left(error))
          case Right(request)  =>
            method(request).map(_.map(b => upickle.default.writeJs(b)(using definition.resRW)))
        }
      }

      override def methodName: String = definition.methodName

      override def introspection: MethodIntrospection = definition.introspection
    }

  private[jsonrpc] def introspect[F[_]: Monad](source: Handler[F]): Handler[F] = {
    val payload = upickle.default.writeJs(source.introspection)
    val introMeta = MethodIntrospection(
      method = s"${source.methodName}.introspect",
      input  = ujson.Obj("type" -> "object", "additionalProperties" -> false),
      output = upickle.default.schema[MethodIntrospection]
    )
    new Handler[F] {
      override def handle(a: ujson.Value): F[Either[JsonRpcError, ujson.Value]] =
        Monad[F].pure(Right(payload))

      override def methodName: String = s"${source.methodName}.introspect"

      override def introspection: MethodIntrospection = introMeta
    }
  }
}

object JsonRpcServer {

  def create[F[_]: MonadThrow](handlers: List[Handler[F]]): JsonRpcServer[F] = {
    val withIntrospects = handlers ++ handlers.map(Handler.introspect(_))
    fromHandlers(withIntrospects)
  }

  def fromHandlers[F[_]: MonadThrow](handlers: List[Handler[F]]): JsonRpcServer[F] = new JsonRpcServer[F] {
    override def handle(request: String): F[String] = {
      val parsed = Try(ujson.read(request)).toEither.left.map(_ => JsonRpcError.ParseError)

      val res: Either[JsonRpcError, F[Either[JsonRpcError, ujson.Value]]] = for {
        json       <- parsed
        rpcRequest <- Try(read[JsonRpcRequest](json)).toEither.left.map(_ => JsonRpcError.InvalidRequest(request))
        handler <- handlers
          .find(_.methodName == rpcRequest.method)
          .toRight(JsonRpcError.MethodNotFound(rpcRequest.method))
      } yield MonadThrow[F].catchNonFatal(handler.handle(rpcRequest.params)).flatten

      val id: String = parsed.toOption.flatMap { r =>
        r.obj.get("id").flatMap {
          case ujson.Str(s) => Some(s)
          case ujson.Num(n) => Some(if (n == n.toLong.toDouble) n.toLong.toString else n.toString)
          case _            => None
        }
      }.getOrElse("")

      def respond(result: Option[ujson.Value], error: Option[JsonRpcError]): String =
        write(JsonRpcResponse(jsonrpc = "2.0", id = id, result = result, error = error))

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
