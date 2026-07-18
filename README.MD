# simple-scala-json-rpc

A lightweight, type-safe [JSON-RPC 2.0](https://www.jsonrpc.org/specification) implementation for Scala 3, with server and client implementations and a functional core built on Cats.

[![](https://jitpack.io/v/yarhrn/simple-scala-json-rpc.svg)](https://jitpack.io/#yarhrn/simple-scala-json-rpc)

## Features

- Type-safe method definitions with compile-time request/response wiring
- Server and client implementations
- Functional approach using Cats type classes (`Functor`, `Monad`, `MonadThrow`)
- Pluggable transport layer (STTP integration included)
- Play JSON for serialization

## Installation

Add the JitPack resolver and the desired module to your `build.sbt`:

```scala
resolvers += "jitpack" at "https://jitpack.io"

// Core module (required)
libraryDependencies += "com.github.yarhrn.simple-scala-json-rpc" %% "simple-scala-json-rpc-core" % "0.1.18"

// STTP transport client (optional)
libraryDependencies += "com.github.yarhrn.simple-scala-json-rpc" %% "simple-scala-json-rpc-sttp" % "0.1.18"
```

## Quick Start

### 1. Define Your API Methods

Each request and response type needs a Play JSON `Format`. Define one with the `Json.format` macro in the companion object (Play JSON does not support Scala 3's `derives` clause):

```scala
import jsonrpc.MethodDefinition
import play.api.libs.json.{Json, OFormat}

case class MultiplyRequest(a: Int, b: Int)
object MultiplyRequest {
  implicit val format: OFormat[MultiplyRequest] = Json.format
}

case class MultiplyResponse(result: Int)
object MultiplyResponse {
  implicit val format: OFormat[MultiplyResponse] = Json.format
}

val Multiply = MethodDefinition.create[MultiplyRequest, MultiplyResponse]("multiply")

// Method with no request parameters — `Unit` uses the built-in empty format,
// brought into scope by importing it explicitly:
import jsonrpc.MethodDefinition.EmtpyResponseFormat

case class StatusResponse(status: String)
object StatusResponse {
  implicit val format: OFormat[StatusResponse] = Json.format
}
val GetStatus = MethodDefinition.create[Unit, StatusResponse]("getStatus")
```

### 2. Server Implementation

Attach a handler to each method and build a server:

```scala
import cats.effect.IO
import jsonrpc.{JsonRpcServer, HandlerResult}

val server = JsonRpcServer.create[IO](
  List(
    Multiply.handler { request =>
      IO(HandlerResult.success(MultiplyResponse(request.a * request.b)))
    },
    GetStatus.handler {
      IO(HandlerResult.success(StatusResponse("healthy")))
    }
  )
)

// Handle an incoming JSON-RPC request
val response: IO[String] = server.handle(jsonRpcRequestString)
```

### 3. Client Implementation

Use the STTP transport client to make JSON-RPC calls:

```scala
import cats.effect.IO
import jsonrpc.client.JsonRpcClient
import jsonrpc.sttp.SttpTransportClient
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

val backend   = AsyncHttpClientCatsBackend[IO]()
val transport = SttpTransportClient[IO](backend, "http://localhost:8080/json-rpc")

given JsonRpcClient[IO] = JsonRpcClient.from(transport)

val result: IO[Either[JsonRpcClientError, MultiplyResponse]] =
  Multiply.execute(MultiplyRequest(10, 5))

// For methods with no parameters
val status: IO[Either[JsonRpcClientError, StatusResponse]] =
  GetStatus.execute
```

### 4. Failing Client (throws exceptions)

If you prefer exceptions over `Either`:

```scala
import jsonrpc.client.FailingJsonRpcClient

given FailingJsonRpcClient[IO] = FailingJsonRpcClient.from(summon[JsonRpcClient[IO]])

// Throws JsonRpcExecuteException on error
val result: IO[MultiplyResponse] = Multiply.executeOrFail(MultiplyRequest(10, 5))
```

## Error Handling

The library provides standard JSON-RPC 2.0 error codes:

| Error            | Code   | Description                |
|------------------|--------|----------------------------|
| `ParseError`     | -32700 | Invalid JSON               |
| `InvalidRequest` | -32600 | Invalid JSON-RPC request   |
| `MethodNotFound` | -32601 | Method does not exist      |
| `InvalidParams`  | -32602 | Invalid method parameters  |
| `InternalError`  | -32603 | Internal server error      |

Return errors from handlers:

```scala
Multiply.handler { request =>
  if (request.b == 0)
    IO(HandlerResult.error(JsonRpcError.InvalidParams))
  else
    IO(HandlerResult.success(MultiplyResponse(request.a / request.b)))
}
```

## Custom Transport Layer

Implement `TransportLayerClient` for a custom HTTP client:

```scala
import jsonrpc.client.{TransportLayerClient, Response}

class MyTransportClient[F[_]] extends TransportLayerClient[F] {
  def execute(request: String): F[Response] = {
    // Send request to server and return Response(statusCode, bodyOption)
    ???
  }
}
```

## Modules

| Module                          | Description                                                            |
|---------------------------------|------------------------------------------------------------------------|
| `simple-scala-json-rpc-core`    | Core library: server, client interfaces, and method definitions        |
| `simple-scala-json-rpc-sttp`    | STTP-based transport client implementation                             |

## Requirements

- Scala 3.8.x
- JDK 21+

## License

MIT License - see [LICENSE](LICENSE) for details.
