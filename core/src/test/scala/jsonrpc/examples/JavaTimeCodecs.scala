package jsonrpc.examples

import upickle.default.{ReadWriter, readwriter}
import upickle.jsonschema.JsonSchema

import java.time.{Duration, Instant}

/** upickle RWs + JSON Schema for `java.time` types used in the API.
  *
  * `Instant`  — ISO-8601 (e.g., `"2026-05-22T09:30:00Z"`)
  * `Duration` — ISO-8601 (e.g., `"PT1H30M"`)
  */
object JavaTimeCodecs {

  implicit val instantRW: ReadWriter[Instant] =
    readwriter[String].bimap[Instant](_.toString, Instant.parse(_))

  implicit val instantSchema: JsonSchema[Instant] = new JsonSchema[Instant] {
    def schema(api: upickle.Api, registry: JsonSchema.Registry): ujson.Value =
      ujson.Obj("type" -> "string", "format" -> "date-time")
  }

  implicit val durationRW: ReadWriter[Duration] =
    readwriter[String].bimap[Duration](_.toString, Duration.parse(_))

  implicit val durationSchema: JsonSchema[Duration] = new JsonSchema[Duration] {
    def schema(api: upickle.Api, registry: JsonSchema.Registry): ujson.Value =
      ujson.Obj("type" -> "string", "format" -> "duration")
  }
}
