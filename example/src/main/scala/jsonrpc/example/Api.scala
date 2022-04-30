package jsonrpc.example

import jsonrpc.MethodDefinition
import play.api.libs.json.Json

object Api {

  case class MultiplyRequest(a: Int, b: Int)

  object MultiplyRequest {
    implicit val MultiplyRequestFormat = Json.format[MultiplyRequest]
  }

  case class MultiplyResponse(res: Int)

  object MultiplyResponse {
    implicit val MultiplyResponseFormat = Json.format[MultiplyResponse]
  }

  def Multiply: MethodDefinition[MultiplyRequest, MultiplyResponse] = MethodDefinition.create("multiply")

}
