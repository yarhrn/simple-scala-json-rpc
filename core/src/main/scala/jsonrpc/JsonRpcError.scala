package jsonrpc

import play.api.libs.json.{JsValue, Json, OFormat}

case class JsonRpcError(code: Int, message: String, data: Option[JsValue]) {
  def render: JsValue = Json.obj("code" -> code, "message" -> message, "data" -> data)
}

object JsonRpcError {
  val ParseError: JsonRpcError = JsonRpcError(-32700, "Parse error", None)

  def InvalidRequest(request: String): JsonRpcError =
    JsonRpcError(-32600, "Invalid Request", Some(Json.obj("request" -> request)))

  def MethodNotFound(method: String): JsonRpcError =
    JsonRpcError(-32601, "Method not found", Some(Json.obj("method" -> method)))

  val InvalidParams: JsonRpcError = JsonRpcError(-32602, "Invalid params", None)

  def InternalError(throwable: Throwable): JsonRpcError =
    JsonRpcError(
      code    = -32603,
      message = "Internal error",
      data    = Some(Json.obj(
        "exception" -> throwable.getClass.getName,
        "message"   -> Option(throwable.getMessage).getOrElse("")
      ))
    )

  implicit val JsonRpcErrorFormat: OFormat[JsonRpcError] = Json.format
}
