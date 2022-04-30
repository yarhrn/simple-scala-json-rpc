package jsonrpc.sttp
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.IORuntime
import org.http4s.ember.server.EmberServerBuilder
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Router

import scala.concurrent.duration._
import jsonrpc.{JsonRpcServer, MethodDefinition}
import play.api.libs.json.Json
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.util.Try


class SttpJsonRpcSpec extends AnyFlatSpec with Matchers {


  "SttpJsonRpc" should "sdfsdf" in {
    val server = JsonRpcServer.create[Try](
      List(
        Api.Multiply.handler(request => Try(Right(Api.MultiplyResponse(request.b * request.a))))
      )
    )

    implicit val client = SttpJsonRpcClient[IO](AsyncHttpClientCatsBackend[IO]().unsafeRunSync(), identity, "http://0.0.0.0:8080/json-rpc")

    implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

    val helloWorldService = HttpRoutes.of[IO] {
      case req @ POST -> Root / "json-rpc"  =>
        req.decodeStrict[String]{
          body =>
            println(body)
            Ok("")
        }
    }

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp( Router("/" -> helloWorldService).orNotFound)
      .build
      .useForever
      .start
      .unsafeToFuture()


    Api.Multiply.execute(Api.MultiplyRequest(10,80)).unsafeRunSync()

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
