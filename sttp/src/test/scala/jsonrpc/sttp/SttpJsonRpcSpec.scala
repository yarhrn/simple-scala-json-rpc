package jsonrpc.sttp

import cats.effect._
import cats.effect.unsafe.IORuntime
import com.comcast.ip4s._
import jsonrpc.client.JsonRpcClient
import jsonrpc.sttp.Api.MultiplyResponse
import jsonrpc.{HandlerResult, JsonRpcServer, MethodDefinition}
import org.http4s.EntityDecoder.collectBinary
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Json, OFormat}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import java.time.Instant

class SttpJsonRpcSpec extends AnyFlatSpec with Matchers {

  "SttpJsonRpc" should "return" in {
    val server = JsonRpcServer.create[IO](
      List(
        Api.Multiply.handler(request => IO(HandlerResult.success(Api.MultiplyResponse(request.b * request.a)))),
        Api.Multiply.handler(request => IO(HandlerResult.success(Api.MultiplyResponse(request.b * request.a)))),
        Api
          .TriggerRebuild
          .handler(IO {
            println("triggered")
            HandlerResult.success(())
          }),
        Api
          .TriggerRebuild
          .handler(IO {
            println("triggered")
            HandlerResult.success(())
          }),
        Api
          .GetInstant
          .handler(IO {
            HandlerResult.success("ass" -> "sdfsdf")
          })
      )
    )
    implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

    implicit val client123: JsonRpcClient[IO] = JsonRpcClient.from(
      SttpTransportClient[IO](AsyncHttpClientCatsBackend[IO]().unsafeRunSync(), "http://0.0.0.0:8080/json-rpc"))

    val helloWorldService = HttpRoutes.of[IO] {
      case req @ POST -> Root / "json-rpc" =>
        req.decodeWith(
          EntityDecoder.decodeBy(MediaRange.`*/*`)(msg =>
            collectBinary(msg)
              .map(chunk => new String(chunk.toArray, msg.charset.getOrElse(Charset.`UTF-8`).nioCharset))),
          strict = true
        ) { body => Ok(server.handle(body)) }
    }

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(Router("/" -> helloWorldService).orNotFound)
      .build
      .useForever
      .start
      .unsafeToFuture()

    val result = Api.Multiply.execute(Api.MultiplyRequest(10, 80)).unsafeRunSync().toOption.get
    Api.TriggerRebuild.execute.unsafeRunSync().toOption.get
    println(Api.GetInstant.execute.unsafeRunSync().toOption.get)
    assert(result == MultiplyResponse(800))
  }

}

object Api {

  import MethodDefinition._

  case class MultiplyRequest(a: Int, b: Int)

  object MultiplyRequest {
    implicit val MultiplyRequestFormat: OFormat[MultiplyRequest] = Json.format[MultiplyRequest]
  }

  case class MultiplyResponse(res: Int)

  object MultiplyResponse {
    implicit val MultiplyResponseFormat: OFormat[MultiplyResponse] = Json.format[MultiplyResponse]
  }

  val Multiply: MethodDefinition[MultiplyRequest, MultiplyResponse] =
    MethodDefinition.create[MultiplyRequest, MultiplyResponse]("multiply")

  val TriggerRebuild: MethodDefinition[Unit, Unit] = MethodDefinition.create("trigger")

  val GetInstant: MethodDefinition[Unit, (String, String)] = MethodDefinition.create("getInstant")

}
