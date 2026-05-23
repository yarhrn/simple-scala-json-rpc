package jsonrpc

import cats.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import upickle.default.ReadWriter
import upickle.jsonschema.JsonSchema

import scala.util.{Failure, Try}

class ServerErrorHandlingSpec extends AnyFlatSpec with Matchers {

  import ServerErrorHandlingSpec._

  private def serverWith(handlers: Handler[Try]*): JsonRpcServer[Try] =
    JsonRpcServer.create[Try](handlers.toList)

  private def call(server: JsonRpcServer[Try], method: String, params: String = "{}"): ujson.Value = {
    val body = server.handle(s"""{"jsonrpc":"2.0","id":"1","method":"$method","params":$params}""").get
    ujson.read(body)
  }

  "JsonRpcServer" should "return InternalError when a handler throws synchronously" in {
    val server = serverWith(
      Throwing.handler[Try](_ => throw new RuntimeException("boom"))
    )
    val r = call(server, "throwing")
    val error = r("error")
    error("code").num.toInt        shouldBe -32603
    error("message").str           shouldBe "Internal error"
    error("data")("exception").str should include("RuntimeException")
    error("data")("message").str   shouldBe "boom"
  }

  it should "return InternalError when a handler returns a failed effect" in {
    val server = serverWith(
      Throwing.handler[Try](_ => Failure(new IllegalStateException("nope")))
    )
    val r = call(server, "throwing")
    val error = r("error")
    error("code").num.toInt        shouldBe -32603
    error("message").str           shouldBe "Internal error"
    error("data")("exception").str should include("IllegalStateException")
    error("data")("message").str   shouldBe "nope"
  }

  it should "still echo the request id on internal errors" in {
    val server = serverWith(
      Throwing.handler[Try](_ => throw new RuntimeException("boom"))
    )
    val r = call(server, "throwing")
    r("id").str shouldBe "1"
  }

  it should "not swallow successful responses" in {
    val server = serverWith(
      Echo.handler[Try](req => Try(HandlerResult.success(EchoResponse(req.value))))
    )
    val r = call(server, "echo", """{"value":"hi"}""")
    r("result")("value").str shouldBe "hi"
    r("error")               shouldBe ujson.Null
  }
}

object ServerErrorHandlingSpec {
  case class EchoRequest(value: String)  derives ReadWriter, JsonSchema
  case class EchoResponse(value: String) derives ReadWriter, JsonSchema

  val Echo: MethodDefinition[EchoRequest, EchoResponse] =
    MethodDefinition.create[EchoRequest, EchoResponse]("echo")

  val Throwing: MethodDefinition[Unit, Unit] =
    MethodDefinition.create[Unit, Unit]("throwing")
}
