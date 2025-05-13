package jsonrpc

import play.api.libs.json.{JsValue, Json, OFormat}

case class JsonRpcError(code: Int, message: String, data: Option[JsValue]) {
  def render: JsValue = Json.obj("code" -> code, "message" -> message, "data" -> data)
}

object JsonRpcError{
  val ParseError: JsonRpcError = JsonRpcError(-32700, "Parse error", None)
  val InvalidRequest: JsonRpcError = JsonRpcError(-32600, "Invalid Request", None)
  val MethodNotFound: JsonRpcError = JsonRpcError(-32601, "Method not found", None)
  val InvalidParams: JsonRpcError = JsonRpcError(-32602, "Invalid params", None)
  val InternalError: JsonRpcError = JsonRpcError(-32603, "Internal error", None)
  implicit val JsonRpcErrorFormat: OFormat[JsonRpcError] = Json.format
}