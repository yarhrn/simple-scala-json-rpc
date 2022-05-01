package jsonrpc.client

trait TransportLayerClient[F[_]] {
  def execute(request: String): F[Response]
}

case class Response(status: Int, body: Option[String])


