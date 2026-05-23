package jsonrpc.examples

import jsonrpc.examples.Common.RamielId
import jsonrpc.examples.JavaTimeCodecs.given
import upickle.default.ReadWriter
import upickle.jsonschema.JsonSchema

import java.time.{Duration, Instant}

object Data {

  case class SessionStartedRequest(ramielId: RamielId, sessionId: String, startedAt: Instant)
      derives ReadWriter, JsonSchema

  case class SessionEndedRequest(
      ramielId: RamielId,
      sessionId: String,
      startedAt: Instant,
      endedAt: Instant)
      derives ReadWriter, JsonSchema

  case class SubscriptionInitiateExpireRequest(premiumInfoId: String)
      derives ReadWriter, JsonSchema

  case class GetStreamReportRequest(id: String) derives ReadWriter, JsonSchema

  case class StreamGame(categoryId: String, name: String, startedAt: Instant, endedAt: Instant)
      derives ReadWriter, JsonSchema

  case class StreamReport(id: String, games: List[StreamGame]) derives ReadWriter, JsonSchema

  case class GetStreamReportResponse(report: StreamReport) derives ReadWriter, JsonSchema

  case class GetPlatformStreamRequest(id: String) derives ReadWriter, JsonSchema

  case class PlatformStream(id: String, games: List[StreamGame]) derives ReadWriter, JsonSchema

  case class GetPlatformStreamResponse(platformStream: PlatformStream)
      derives ReadWriter, JsonSchema

  case class RetrieveStreamClipRequest(id: String) derives ReadWriter, JsonSchema

  case class StreamClip(id: String, url: String, viewCount: Int) derives ReadWriter, JsonSchema

  case class RetrieveStreamClipResponse(clip: Option[StreamClip] = None) derives ReadWriter, JsonSchema

  // ---- webhook ----- //
  case class WebhookHeader(key: String, value: String) derives ReadWriter, JsonSchema

  case class WebhookRange(from: Int, to: Int) derives ReadWriter, JsonSchema

  case class WebhookRangeConfiguration(times: Int, window: Duration, ranges: List[WebhookRange])
      derives ReadWriter, JsonSchema

  case class WebhookConfiguration(
      id: String,
      ramielId: RamielId,
      uri: String,
      headers: List[WebhookHeader],
      bodyTemplate: String,
      rangeConfiguration: Option[WebhookRangeConfiguration] = None
  ) derives ReadWriter, JsonSchema

  case class CreateOrUpdateWebhookRequest(webhook: WebhookConfiguration)
      derives ReadWriter, JsonSchema

  case class CreateOrUpdateWebhookResponse(webhook: WebhookConfiguration)
      derives ReadWriter, JsonSchema

  case class GetWebhooksRequest(ramielId: RamielId) derives ReadWriter, JsonSchema

  case class GetWebhooksResponse(webhooks: List[WebhookConfiguration])
      derives ReadWriter, JsonSchema

  case class DeleteWebhookRequest(ramielId: RamielId, id: String) derives ReadWriter, JsonSchema

  case class DeleteWebhookResponse(webhook: Option[WebhookConfiguration] = None)
      derives ReadWriter, JsonSchema
}
