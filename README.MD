# simple-scala-json-rpc

A lightweight, type-safe [JSON-RPC 2.0](https://www.jsonrpc.org/specification) implementation for Scala, supporting both server and client implementations with a functional programming approach using Cats.

[![](https://jitpack.io/v/yarhrn/simple-scala-json-rpc.svg)](https://jitpack.io/#yarhrn/simple-scala-json-rpc)

## Features

- Type-safe method definitions with compile-time request/response validation
- Server and client implementations
- Functional approach using Cats type classes (`Functor`, `Monad`, `MonadError`)
- Cross-compiled for Scala 2.13 and Scala 3
- Pluggable transport layer (STTP integration included)
- Play JSON for serialization

## Installation

Add JitPack resolver and the desired module to your `build.sbt`:

```scala
resolvers += "jitpack" at "https://jitpack.io"

// Core module (required)
libraryDependencies += "com.github.yarhrn.simple-scala-json-rpc" %% "simple-scala-json-rpc-core" % "0.1.14"

// STTP transport client (optional)
libraryDependencies += "com.github.yarhrn.simple-scala-json-rpc" %% "simple-scala-json-rpc-sttp" % "0.1.14"
```

## Quick Start

### 1. Define Your API Methods

Create type-safe method definitions with request and response types:

```scala
import jsonrpc.MethodDefinition
import play.api.libs.json.{Json, OFormat}

// Request and response case classes
case class MultiplyRequest(a: Int, b: Int)
object MultiplyRequest {
  implicit val format: OFormat[MultiplyRequest] = Json.format
}

case class MultiplyResponse(result: Int)
object MultiplyResponse {
  implicit val format: OFormat[MultiplyResponse] = Json.format
}

// Method definition
val Multiply = MethodDefinition.create[MultiplyRequest, MultiplyResponse]("multiply")

// Method with no request parameters
val GetStatus = MethodDefinition.create[Unit, StatusResponse]("getStatus")
```

### 2. Server Implementation

Create handlers for your methods and build a server:

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

// Handle incoming JSON-RPC request
val response: IO[String] = server.handle(jsonRpcRequestString)
```

### 3. Client Implementation

Use the STTP transport client to make JSON-RPC calls:

```scala
import cats.effect.IO
import jsonrpc.client.JsonRpcClient
import jsonrpc.sttp.SttpTransportClient
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

// Create STTP backend and transport client
val backend = AsyncHttpClientCatsBackend[IO]()
val transport = SttpTransportClient[IO](backend, "http://localhost:8080/json-rpc")

// Create JSON-RPC client
implicit val client: JsonRpcClient[IO] = JsonRpcClient.from(transport)

// Execute methods
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

implicit val failingClient: FailingJsonRpcClient[IO] = 
  FailingJsonRpcClient.from(client)

// Throws JsonRpcExecuteException on error
val result: IO[MultiplyResponse] = Multiply.executeOrFail(MultiplyRequest(10, 5))
```

## Error Handling

The library provides standard JSON-RPC 2.0 error codes:

| Error | Code | Description |
|-------|------|-------------|
| `ParseError` | -32700 | Invalid JSON |
| `InvalidRequest` | -32600 | Invalid JSON-RPC request |
| `MethodNotFound` | -32601 | Method does not exist |
| `InvalidParams` | -32602 | Invalid method parameters |
| `InternalError` | -32603 | Internal server error |

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

Implement `TransportLayerClient` for custom HTTP clients:

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

| Module | Description |
|--------|-------------|
| `simple-scala-json-rpc-core` | Core library with server, client interfaces, and method definitions |
| `simple-scala-json-rpc-sttp` | STTP-based transport client implementation |

## Requirements

- Scala 2.13.x or 3.3.x
- JDK 11+

## License

MIT License - see [LICENSE](LICENSE) for details.
