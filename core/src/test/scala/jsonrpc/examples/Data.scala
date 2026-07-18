package jsonrpc.examples

import jsonrpc.examples.Common.RamielId

import java.time.{Duration, Instant}

object Data {

  case class SessionStartedRequest(ramielId: RamielId, sessionId: String, startedAt: Instant)

  case class SessionEndedRequest(
      ramielId: RamielId,
      sessionId: String,
      startedAt: Instant,
      endedAt: Instant)

  case class SubscriptionInitiateExpireRequest(premiumInfoId: String)

  case class GetStreamReportRequest(id: String)

  case class StreamGame(categoryId: String, name: String, startedAt: Instant, endedAt: Instant)

  case class StreamReport(id: String, games: List[StreamGame])

  case class GetStreamReportResponse(report: StreamReport)

  case class GetPlatformStreamRequest(id: String)

  case class PlatformStream(id: String, games: List[StreamGame])

  case class GetPlatformStreamResponse(platformStream: PlatformStream)

  case class RetrieveStreamClipRequest(id: String)

  case class StreamClip(id: String, url: String, viewCount: Int)

  case class RetrieveStreamClipResponse(clip: Option[StreamClip] = None)

  // ---- webhook ----- //
  case class WebhookHeader(key: String, value: String)

  case class WebhookRange(from: Int, to: Int)

  case class WebhookRangeConfiguration(times: Int, window: Duration, ranges: List[WebhookRange])

  case class WebhookConfiguration(
      id: String,
      ramielId: RamielId,
      uri: String,
      headers: List[WebhookHeader],
      bodyTemplate: String,
      rangeConfiguration: Option[WebhookRangeConfiguration] = None
  )

  case class CreateOrUpdateWebhookRequest(webhook: WebhookConfiguration)

  case class CreateOrUpdateWebhookResponse(webhook: WebhookConfiguration)

  case class GetWebhooksRequest(ramielId: RamielId)

  case class GetWebhooksResponse(webhooks: List[WebhookConfiguration])

  case class DeleteWebhookRequest(ramielId: RamielId, id: String)

  case class DeleteWebhookResponse(webhook: Option[WebhookConfiguration] = None)
}
