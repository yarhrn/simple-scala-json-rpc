package jsonrpc

import cats.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json, OFormat}

import scala.util.{Failure, Try}

class ServerErrorHandlingSpec extends AnyFlatSpec with Matchers {

  import ServerErrorHandlingSpec._

  private def serverWith(handlers: Handler[Try]*): JsonRpcServer[Try] =
    JsonRpcServer.create[Try](handlers.toList)

  private def call(server: JsonRpcServer[Try], method: String, params: String = "{}"): JsValue = {
    val body = server.handle(s"""{"jsonrpc":"2.0","id":"1","method":"$method","params":$params}""").get
    Json.parse(body)
  }

  "JsonRpcServer" should "return InternalError when a handler throws synchronously" in {
    val server = serverWith(
      Throwing.handler[Try](_ => throw new RuntimeException("boom"))
    )
    val r = call(server, "throwing")
    (r \ "error" \ "code").as[Int]                  shouldBe -32603
    (r \ "error" \ "message").as[String]            shouldBe "Internal error"
    (r \ "error" \ "data" \ "exception").as[String] should include("RuntimeException")
    (r \ "error" \ "data" \ "message").as[String]   shouldBe "boom"
  }

  it should "return InternalError when a handler returns a failed effect" in {
    val server = serverWith(
      Throwing.handler[Try](_ => Failure(new IllegalStateException("nope")))
    )
    val r = call(server, "throwing")
    (r \ "error" \ "code").as[Int]                  shouldBe -32603
    (r \ "error" \ "message").as[String]            shouldBe "Internal error"
    (r \ "error" \ "data" \ "exception").as[String] should include("IllegalStateException")
    (r \ "error" \ "data" \ "message").as[String]   shouldBe "nope"
  }

  it should "still echo the request id on internal errors" in {
    val server = serverWith(
      Throwing.handler[Try](_ => throw new RuntimeException("boom"))
    )
    val r = call(server, "throwing")
    (r \ "id").as[String] shouldBe "1"
  }

  it should "not swallow successful responses" in {
    val server = serverWith(
      Echo.handler[Try](req => Try(HandlerResult.success(EchoResponse(req.value))))
    )
    val r = call(server, "echo", """{"value":"hi"}""")
    (r \ "result" \ "value").as[String] shouldBe "hi"
    (r \ "error").toOption              shouldBe None
  }

  it should "omit the result key on errors (per JSON-RPC 2.0)" in {
    val server = serverWith(
      Throwing.handler[Try](_ => throw new RuntimeException("boom"))
    )
    val r = call(server, "throwing")
    (r \ "result").toOption shouldBe None
    (r \ "error").toOption  shouldBe defined
  }
}

object ServerErrorHandlingSpec {

  // Brings `EmtpyResponseFormat` into scope, providing `Format[Unit]` for parameterless methods.
  import MethodDefinition.EmtpyResponseFormat

  case class EchoRequest(value: String)
  object EchoRequest {
    implicit val format: OFormat[EchoRequest] = Json.format
  }

  case class EchoResponse(value: String)
  object EchoResponse {
    implicit val format: OFormat[EchoResponse] = Json.format
  }

  val Echo: MethodDefinition[EchoRequest, EchoResponse] =
    MethodDefinition.create[EchoRequest, EchoResponse]("echo")

  val Throwing: MethodDefinition[Unit, Unit] =
    MethodDefinition.create[Unit, Unit]("throwing")
}
