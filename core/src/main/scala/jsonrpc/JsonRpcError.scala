package jsonrpc

import play.api.libs.json.{JsValue, Json}

case class JsonRpcError(code: Int, message: String, data: JsValue) {
  def render: JsValue = Json.obj("code" -> code, "message" -> message, "data" -> data)
}