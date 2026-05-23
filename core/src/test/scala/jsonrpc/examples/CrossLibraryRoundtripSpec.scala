package jsonrpc.examples

import jsonrpc.examples.Common.RamielId
import jsonrpc.examples.Data._
import jsonrpc.examples.JavaTimeCodecs.given
import jsonrpc.examples.PlayJsonFormats._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Format, Json}
import upickle.default.{ReadWriter, read, write}

import java.time.{Duration, Instant}

/** For every case class in [[Data]] and the shared types ([[RamielId]], [[Instant]], [[Duration]]),
  * check that play-json and upickle agree on the on-wire JSON:
  *   1. value → play-json bytes → upickle decode → value
  *   2. value → upickle bytes  → play-json decode → value
  *
  * If both round-trips succeed the two libraries are wire-compatible for that type.
  */
class CrossLibraryRoundtripSpec extends AnyFlatSpec with Matchers {

  private def playToUpickle[A: Format: ReadWriter](label: String, value: A): Unit = {
    val playJson = Json.stringify(Json.toJson(value))
    val decoded  = read[A](playJson)
    withClue(s"[$label] play-json -> upickle, json=$playJson: ") { decoded shouldBe value }
  }

  private def upickleToPlay[A: Format: ReadWriter](label: String, value: A): Unit = {
    val upickleJson = write(value)
    val parsed      = Json.parse(upickleJson)
    val decoded     = parsed.as[A]
    withClue(s"[$label] upickle -> play-json, json=$upickleJson: ") { decoded shouldBe value }
  }

  private def bothDirections[A: Format: ReadWriter](label: String, value: A): Unit = {
    playToUpickle(label, value)
    upickleToPlay(label, value)
  }

  // ---- sample values ----
  private val t0 = Instant.parse("2026-05-22T09:00:00Z")
  private val t1 = Instant.parse("2026-05-22T10:30:00Z")

  private val ramielId   = RamielId("ram-1")
  private val streamGame = StreamGame("cat-1", "Game One", t0, t1)
  private val webhookHeader        = WebhookHeader("Authorization", "Bearer abc")
  private val webhookRange         = WebhookRange(60, 120)
  private val rangeConfiguration   = WebhookRangeConfiguration(3, Duration.ofMinutes(10), List(webhookRange))
  private val webhookConfiguration = WebhookConfiguration(
    id           = "w-1",
    ramielId     = ramielId,
    uri          = "https://example.test/hook",
    headers      = List(webhookHeader),
    bodyTemplate = "{}",
    rangeConfiguration = Some(rangeConfiguration)
  )

  "shared primitive types" should "round-trip in both directions" in {
    bothDirections("RamielId",  ramielId)
    bothDirections("Instant",   t0)
    bothDirections("Duration",  Duration.ofMinutes(10))
  }

  "Data case classes" should "round-trip in both directions" in {
    bothDirections("SessionStartedRequest",            SessionStartedRequest(ramielId, "sess-1", t0))
    bothDirections("SessionEndedRequest",              SessionEndedRequest(ramielId, "sess-1", t0, t1))
    bothDirections("SubscriptionInitiateExpireRequest", SubscriptionInitiateExpireRequest("prem-1"))
    bothDirections("GetStreamReportRequest",           GetStreamReportRequest("s-1"))
    bothDirections("StreamGame",                       streamGame)
    bothDirections("StreamReport",                     StreamReport("rep-1", List(streamGame)))
    bothDirections("GetStreamReportResponse",          GetStreamReportResponse(StreamReport("rep-1", List(streamGame))))
    bothDirections("GetPlatformStreamRequest",         GetPlatformStreamRequest("p-1"))
    bothDirections("PlatformStream",                   PlatformStream("p-1", List(streamGame)))
    bothDirections("GetPlatformStreamResponse",        GetPlatformStreamResponse(PlatformStream("p-1", List(streamGame))))
    bothDirections("RetrieveStreamClipRequest",        RetrieveStreamClipRequest("c-1"))
    bothDirections("StreamClip",                       StreamClip("c-1", "https://example.test/c-1.mp4", 42))
    bothDirections("RetrieveStreamClipResponse",       RetrieveStreamClipResponse(Some(StreamClip("c-1", "u", 1))))
    bothDirections("RetrieveStreamClipResponse(None)", RetrieveStreamClipResponse(None))
    bothDirections("WebhookHeader",                    webhookHeader)
    bothDirections("WebhookRange",                     webhookRange)
    bothDirections("WebhookRangeConfiguration",        rangeConfiguration)
    bothDirections("WebhookConfiguration",             webhookConfiguration)
    bothDirections("WebhookConfiguration(rangeConfig=None)",
      webhookConfiguration.copy(rangeConfiguration = None))
    bothDirections("CreateOrUpdateWebhookRequest",     CreateOrUpdateWebhookRequest(webhookConfiguration))
    bothDirections("CreateOrUpdateWebhookResponse",    CreateOrUpdateWebhookResponse(webhookConfiguration))
    bothDirections("GetWebhooksRequest",               GetWebhooksRequest(ramielId))
    bothDirections("GetWebhooksResponse",              GetWebhooksResponse(List(webhookConfiguration)))
    bothDirections("DeleteWebhookRequest",             DeleteWebhookRequest(ramielId, "w-1"))
    bothDirections("DeleteWebhookResponse(Some)",      DeleteWebhookResponse(Some(webhookConfiguration)))
    bothDirections("DeleteWebhookResponse(None)",      DeleteWebhookResponse(None))
  }
}
