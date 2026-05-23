package jsonrpc

import cats.Monad
import jsonrpc.client.{FailingJsonRpcClient, JsonRpcClient, JsonRpcClientError}
import upickle.default.ReadWriter
import upickle.jsonschema.JsonSchema

case class MethodDefinition[Req, Res](
    methodName: String,
    reqRW: ReadWriter[Req],
    resRW: ReadWriter[Res],
    inputSchema: JsonSchema[Req],
    outputSchema: JsonSchema[Res]) {

  /** Top-level Draft 2020-12 schema document for the request type. */
  lazy val renderedInputSchema: ujson.Value =
    JsonSchema.schemaFor[Req](upickle.default)(using inputSchema)

  /** Top-level Draft 2020-12 schema document for the response type. */
  lazy val renderedOutputSchema: ujson.Value =
    JsonSchema.schemaFor[Res](upickle.default)(using outputSchema)

  def handler[F[_]: Monad](method: Req => F[Either[JsonRpcError, Res]]): Handler[F] =
    Handler.create(this, method)

  def handler[F[_]: Monad](method: F[Either[JsonRpcError, Res]])(implicit ev: Req =:= Unit): Handler[F] =
    Handler.create(this, (_: Req) => method)

  def execute[F[_]](request: Req)(implicit client: JsonRpcClient[F]): F[Either[JsonRpcClientError, Res]] =
    client.execute(this, request)

  def execute[F[_]](implicit ev: Req =:= Unit, client: JsonRpcClient[F]): F[Either[JsonRpcClientError, Res]] =
    client.execute(this, ev.flip.apply(()))

  def executeOrFail[F[_]](request: Req)(implicit client: FailingJsonRpcClient[F]): F[Res] =
    client.executeOrFail(this, request)

  def introspection: MethodIntrospection =
    MethodIntrospection(methodName, renderedInputSchema, renderedOutputSchema)
}

object MethodDefinition {

  def create[A, B](methodName: String)(
      using reqRW: ReadWriter[A],
      resRW: ReadWriter[B],
      reqSchema: JsonSchema[A],
      resSchema: JsonSchema[B]
  ): MethodDefinition[A, B] =
    new MethodDefinition[A, B](methodName, reqRW, resRW, reqSchema, resSchema)
}

case class MethodIntrospection(method: String, input: ujson.Value, output: ujson.Value)
    derives ReadWriter, JsonSchema
