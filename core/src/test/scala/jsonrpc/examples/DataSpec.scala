package jsonrpc.examples

import jsonrpc.examples.Common.RamielId
import jsonrpc.examples.Data._
import jsonrpc.examples.JavaTimeCodecs.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import upickle.default.{read, write}
import upickle.jsonschema.schema

import java.time.{Duration, Instant}

class DataSpec extends AnyFlatSpec with Matchers {

  "RamielId" should "serialize as a bare JSON string" in {
    write(RamielId("alice")) shouldBe "\"alice\""
    read[RamielId]("\"alice\"") shouldBe RamielId("alice")
  }

  it should "have a string schema" in {
    val s = upickle.default.schema[RamielId]
    // simple type, no $defs registered
    s("type").str shouldBe "string"
  }

  "Instant" should "round-trip as an ISO-8601 string" in {
    val inst = Instant.parse("2026-05-22T09:30:00Z")
    write(inst)              shouldBe "\"2026-05-22T09:30:00Z\""
    read[Instant]("\"2026-05-22T09:30:00Z\"") shouldBe inst
  }

  "Duration" should "round-trip as an ISO-8601 string" in {
    val d = Duration.ofMinutes(90)
    write(d)                shouldBe "\"PT1H30M\""
    read[Duration]("\"PT1H30M\"") shouldBe d
  }

  "SessionStartedRequest" should "round-trip" in {
    val v = SessionStartedRequest(
      ramielId  = RamielId("ram-1"),
      sessionId = "sess-1",
      startedAt = Instant.parse("2026-05-22T09:00:00Z")
    )
    val json = write(v)
    json shouldBe """{"ramielId":"ram-1","sessionId":"sess-1","startedAt":"2026-05-22T09:00:00Z"}"""
    read[SessionStartedRequest](json) shouldBe v
  }

  "WebhookConfiguration" should "round-trip including nested duration and option" in {
    val v = WebhookConfiguration(
      id           = "w-1",
      ramielId     = RamielId("ram-1"),
      uri          = "https://example.test/hook",
      headers      = List(WebhookHeader("Authorization", "Bearer xyz")),
      bodyTemplate = "{}",
      rangeConfiguration = Some(
        WebhookRangeConfiguration(
          times  = 3,
          window = Duration.ofMinutes(10),
          ranges = List(WebhookRange(60, 120), WebhookRange(120, 180))
        )
      )
    )
    val json = write(v)
    read[WebhookConfiguration](json) shouldBe v
  }

  "WebhookConfiguration schema" should "be a top-level Draft 2020-12 doc with $defs" in {
    val s = upickle.default.schema[WebhookConfiguration]
    s("$schema").str should include("json-schema.org")
    val defs = s("$defs").obj
    val key  = defs.keys.find(_.endsWith("WebhookConfiguration")).getOrElse(fail("no schema for WebhookConfiguration"))
    val obj  = defs(key).obj

    obj("type").str shouldBe "object"
    val props = obj("properties").obj
    // ramielId is a flat string
    props("ramielId")("type").str shouldBe "string"
    // headers is array of WebhookHeader refs
    props("headers")("type").str  shouldBe "array"
    // rangeConfiguration is optional → not in required, schema is anyOf with null
    val required = obj("required").arr.map(_.str).toSet
    required                                shouldBe Set("id", "ramielId", "uri", "headers", "bodyTemplate")
    required.contains("rangeConfiguration") shouldBe false
  }
}
