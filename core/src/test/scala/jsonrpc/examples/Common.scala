package jsonrpc.examples

import upickle.default.{ReadWriter, readwriter}
import upickle.jsonschema.JsonSchema

object Common {

  /** Flat-string wrapper. Serializes as the bare String, not as `{"id": "..."}`. */
  case class RamielId(id: String)

  object RamielId {
    implicit val rw: ReadWriter[RamielId] =
      readwriter[String].bimap[RamielId](_.id, RamielId.apply)

    implicit val schema: JsonSchema[RamielId] = new JsonSchema[RamielId] {
      def schema(api: upickle.Api, registry: JsonSchema.Registry): ujson.Value =
        ujson.Obj("type" -> "string")
    }
  }
}
