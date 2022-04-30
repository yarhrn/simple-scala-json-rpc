package jsonrpc


trait JsonRpcClient[F[_]] {
  def execute[A, B](methodDefinition: MethodDefinition[A, B], request: A): F[B]
}
