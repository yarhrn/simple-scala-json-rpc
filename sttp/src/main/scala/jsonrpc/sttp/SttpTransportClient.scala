package jsonrpc.sttp

import cats.Functor
import jsonrpc.client.{Response, TransportLayerClient}
import jsonrpc.sttp.SttpTransportClient.JsonRpcSttpRequest
import sttp.client3.{Request, SttpBackend, basicRequest}
import sttp.model.Uri
import cats.implicits._

object SttpTransportClient {
  type JsonRpcSttpRequest = Request[Either[String, String], Any]
}

case class SttpTransportClient[F[_]](
    sttp: SttpBackend[F, Any],
    url: String,
    adapt: JsonRpcSttpRequest => JsonRpcSttpRequest = identity)(implicit F: Functor[F])
    extends TransportLayerClient[F] {
  override def execute(request: String): F[Response] = {
    val sttpRequest = basicRequest
      .post(Uri.parse(url).right.get) // todo fix?
      .body(request)
      .header("Content-Type", "application/json")
    sttp
      .send(adapt(sttpRequest))
      .map(response =>
        Response(response.code.code, response.body.left.toOption.orElse(response.body.toOption))) // todo improve??
  }
}
