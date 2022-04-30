package jsonrpc.example

import jsonrpc.JsonRpcServer
import jsonrpc.example.Api.MultiplyResponse

import scala.util.Try

object Server {
  val server = JsonRpcServer.create[Try](
    List(
      Api.Multiply.handler(request => Try(Right(MultiplyResponse(request.b * request.a))))
    )
  )



}
