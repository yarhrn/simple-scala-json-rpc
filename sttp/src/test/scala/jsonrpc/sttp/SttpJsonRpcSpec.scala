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
import play.api.libs.json.Json
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend


class SttpJsonRpcSpec extends AnyFlatSpec with Matchers {


  "SttpJsonRpc" should "sdfsdf" in {
    val server = JsonRpcServer.create[IO](
      List(
        Api.Multiply.handler(request => IO(HandlerResult.success(Api.MultiplyResponse(request.b * request.a)))),
        Api.Multiply.handler(request => IO(HandlerResult.success(Api.MultiplyResponse(request.b * request.a)))),
      )
    )
    implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

    implicit val client123: JsonRpcClient[IO] = JsonRpcClient.from(SttpTransportClient[IO](AsyncHttpClientCatsBackend[IO]().unsafeRunSync(), "http://0.0.0.0:8080/json-rpc"))


    val helloWorldService = HttpRoutes.of[IO] {
      case req@POST -> Root / "json-rpc" =>
        req.decodeWith(EntityDecoder.decodeBy(MediaRange.`*/*`)(msg =>
          collectBinary(msg).map(chunk =>
            new String(chunk.toArray, msg.charset.getOrElse(Charset.`UTF-8`).nioCharset)
          )
        ), true) {
          body =>
            Ok(server.handle(body))
        }
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
    assert(result == MultiplyResponse(800))
  }

}

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
