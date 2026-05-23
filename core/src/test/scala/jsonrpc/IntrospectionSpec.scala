package jsonrpc

import cats.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import upickle.default.ReadWriter
import upickle.jsonschema.JsonSchema

import scala.util.Try

class IntrospectionSpec extends AnyFlatSpec with Matchers {

  import IntrospectionSpec._

  private val server: JsonRpcServer[Try] = JsonRpcServer.create[Try](
    List(
      Multiply.handler[Try](req => Try(HandlerResult.success(MultiplyResponse(req.a * req.b)))),
      Ping.handler[Try](Try(HandlerResult.success(())))
    )
  )

  private def call(method: String, params: String = "{}"): ujson.Value =
    ujson.read(server.handle(s"""{"jsonrpc":"2.0","id":"1","method":"$method","params":$params}""").get)

  "JsonRpcServer.create" should "auto-register <method>.introspect for each method" in {
    val r = call("multiply.introspect")("result")
    r("method").str shouldBe "multiply"
    r("input")("$schema").str  should include("json-schema.org")
    r("output")("$schema").str should include("json-schema.org")

    // Input schema should describe MultiplyRequest somewhere in $defs
    val inputDefs = r("input").obj.get("$defs").map(_.obj.keys.toSet).getOrElse(Set.empty)
    inputDefs.exists(_.endsWith("MultiplyRequest")) shouldBe true

    val multiplyDefKey = inputDefs.find(_.endsWith("MultiplyRequest")).get
    val def0           = r("input")("$defs")(multiplyDefKey).obj
    def0("type").str                       shouldBe "object"
    def0("properties")("a")("type").str    shouldBe "integer"
    def0("properties")("b")("type").str    shouldBe "integer"
    def0("required").arr.map(_.str).toSet  shouldBe Set("a", "b")
  }

  it should "register introspection for Unit-input methods too" in {
    val r = call("ping.introspect")("result")
    r("method").str          shouldBe "ping"
    r("input")("type").str   shouldBe "null"
    r("output")("type").str  shouldBe "null"
  }

  it should "still dispatch the underlying method normally" in {
    val r = call("multiply", """{"a":3,"b":4}""")
    r("result")("res").num.toInt shouldBe 12
  }
}

object IntrospectionSpec {
  case class MultiplyRequest(a: Int, b: Int) derives ReadWriter, JsonSchema
  case class MultiplyResponse(res: Int)      derives ReadWriter, JsonSchema

  val Multiply: MethodDefinition[MultiplyRequest, MultiplyResponse] =
    MethodDefinition.create[MultiplyRequest, MultiplyResponse]("multiply")

  val Ping: MethodDefinition[Unit, Unit] = MethodDefinition.create[Unit, Unit]("ping")
}
