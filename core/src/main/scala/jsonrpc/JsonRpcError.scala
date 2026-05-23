package jsonrpc

import upickle.default.ReadWriter
import upickle.jsonschema.JsonSchema

case class JsonRpcError(code: Int, message: String, data: Option[ujson.Value])
    derives ReadWriter, JsonSchema

object JsonRpcError {
  val ParseError: JsonRpcError = JsonRpcError(-32700, "Parse error", None)

  def InvalidRequest(request: String): JsonRpcError =
    JsonRpcError(-32600, "Invalid Request", Some(ujson.Obj("request" -> request)))

  def MethodNotFound(method: String): JsonRpcError =
    JsonRpcError(-32601, "Method not found", Some(ujson.Obj("method" -> method)))

  val InvalidParams: JsonRpcError = JsonRpcError(-32602, "Invalid params", None)

  def InternalError(throwable: Throwable): JsonRpcError =
    JsonRpcError(
      code    = -32603,
      message = "Internal error",
      data    = Some(ujson.Obj(
        "exception" -> throwable.getClass.getName,
        "message"   -> Option(throwable.getMessage).getOrElse("")
      ))
    )
}
