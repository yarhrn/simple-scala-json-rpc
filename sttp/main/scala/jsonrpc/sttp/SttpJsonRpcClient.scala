package jsonrpc.sttp

import cats.effect.IO
import jsonrpc.sttp.SttpJsonRpcClient.JsonRpcSttpRequest
import jsonrpc.{JsonRpcClient, MethodDefinition}
import play.api.libs.json.Json
import sttp.client3.{Request, SttpBackend, basicRequest}
import sttp.model.Uri

import java.util.UUID

object SttpJsonRpcClient {
  type JsonRpcSttpRequest = Request[Either[String, String], Any]
}

class SttpJsonRpcClient(sttp: SttpBackend[IO, Any],
                        adapt: JsonRpcSttpRequest => JsonRpcSttpRequest,
                        url: String) extends JsonRpcClient[IO] {
  override def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): IO[B] = {
    val jsonRpcRequest = Json.obj(
      "jsonrpc" -> "2.0",
      "method" -> methodDefinition.methodName,
      "id" -> UUID.randomUUID().toString,
      "request" -> methodDefinition.req.writes(request))

    val sttpRequest = basicRequest.post(Uri(url))
      .body(Json.stringify(jsonRpcRequest))
      .header("Content-Type", "application/json")

    sttp.send(adapt(sttpRequest))
      .map(response => methodDefinition.res.reads(Json.parse(response.body.toOption.get)).get) // todo fix
  }
}

