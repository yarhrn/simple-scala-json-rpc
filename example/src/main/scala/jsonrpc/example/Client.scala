package jsonrpc.example

import cats.Id
import jsonrpc.{FailingJsonRpcClient, JsonRpcClient}
import jsonrpc.example.Api.MultiplyRequest

import scala.util.Try

object Client {
  implicit val client: JsonRpcClient[Try] = ???
  implicit val client1: FailingJsonRpcClient[Try] = ???

  Api.Multiply.execute(MultiplyRequest(1, 2))

  Api.Multiply.executeOrFail(MultiplyRequest(1, 2)).get.res

}
