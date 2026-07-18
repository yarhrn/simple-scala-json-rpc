package jsonrpc.examples

import jsonrpc.examples.Common.RamielId
import jsonrpc.examples.Data._
import jsonrpc.examples.PlayJsonFormats._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Format, Json}

import java.time.{Duration, Instant}

class DataSpec extends AnyFlatSpec with Matchers {

  private def roundTrip[A: Format](value: A): A =
    Json.parse(Json.stringify(Json.toJson(value))).as[A]

  "RamielId" should "serialize as a bare JSON string" in {
    Json.stringify(Json.toJson(RamielId("alice"))) shouldBe "\"alice\""
    Json.parse("\"alice\"").as[RamielId]           shouldBe RamielId("alice")
  }

  "Instant" should "round-trip as an ISO-8601 string" in {
    val inst = Instant.parse("2026-05-22T09:30:00Z")
    Json.stringify(Json.toJson(inst))                  shouldBe "\"2026-05-22T09:30:00Z\""
    Json.parse("\"2026-05-22T09:30:00Z\"").as[Instant] shouldBe inst
  }

  "Duration" should "round-trip" in {
    val d = Duration.ofMinutes(90)
    roundTrip(d) shouldBe d
  }

  "SessionStartedRequest" should "round-trip" in {
    val v = SessionStartedRequest(
      ramielId  = RamielId("ram-1"),
      sessionId = "sess-1",
      startedAt = Instant.parse("2026-05-22T09:00:00Z")
    )
    val json = Json.stringify(Json.toJson(v))
    json shouldBe """{"ramielId":"ram-1","sessionId":"sess-1","startedAt":"2026-05-22T09:00:00Z"}"""
    Json.parse(json).as[SessionStartedRequest] shouldBe v
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
    roundTrip(v)                              shouldBe v
    roundTrip(v.copy(rangeConfiguration = None)) shouldBe v.copy(rangeConfiguration = None)
  }
}
