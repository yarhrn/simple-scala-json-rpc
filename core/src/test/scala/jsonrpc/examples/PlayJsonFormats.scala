package jsonrpc.examples

import jsonrpc.examples.Common.RamielId
import jsonrpc.examples.Data._
import play.api.libs.json._

import java.time.{Duration, Instant}

/** play-json [[Format]] instances for the same case classes that derive upickle's `ReadWriter`,
  * used by [[CrossLibraryRoundtripSpec]] to verify on-wire compatibility.
  */
object PlayJsonFormats {

  implicit val ramielIdFormat: Format[RamielId] = Format(
    implicitly[Reads[String]].map(RamielId(_)),
    implicitly[Writes[String]].contramap((r: RamielId) => r.id)
  )

  implicit val instantFormat: Format[Instant] = Format(
    Reads.DefaultInstantReads,
    Writes.DefaultInstantWrites
  )

  implicit val javaDurationFormat: Format[Duration] = Format(
    Reads.DefaultJavaDurationReads,
    Writes.javaDurationWrites
  )

  implicit val sessionStartedFmt: OFormat[SessionStartedRequest]                         = Json.format
  implicit val sessionEndedFmt: OFormat[SessionEndedRequest]                             = Json.format
  implicit val subscriptionInitiateFmt: OFormat[SubscriptionInitiateExpireRequest]       = Json.format
  implicit val getStreamReportReqFmt: OFormat[GetStreamReportRequest]                    = Json.format
  implicit val streamGameFmt: OFormat[StreamGame]                                        = Json.format
  implicit val streamReportFmt: OFormat[StreamReport]                                    = Json.format
  implicit val getStreamReportResFmt: OFormat[GetStreamReportResponse]                   = Json.format
  implicit val getPlatformStreamReqFmt: OFormat[GetPlatformStreamRequest]                = Json.format
  implicit val platformStreamFmt: OFormat[PlatformStream]                                = Json.format
  implicit val getPlatformStreamResFmt: OFormat[GetPlatformStreamResponse]               = Json.format
  implicit val retrieveStreamClipReqFmt: OFormat[RetrieveStreamClipRequest]              = Json.format
  implicit val streamClipFmt: OFormat[StreamClip]                                        = Json.format
  implicit val retrieveStreamClipResFmt: OFormat[RetrieveStreamClipResponse]             = Json.format
  implicit val webhookHeaderFmt: OFormat[WebhookHeader]                                  = Json.format
  implicit val webhookRangeFmt: OFormat[WebhookRange]                                    = Json.format
  implicit val webhookRangeConfigurationFmt: OFormat[WebhookRangeConfiguration]          = Json.format
  implicit val webhookConfigurationFmt: OFormat[WebhookConfiguration]                    = Json.format
  implicit val createOrUpdateWebhookReqFmt: OFormat[CreateOrUpdateWebhookRequest]        = Json.format
  implicit val createOrUpdateWebhookResFmt: OFormat[CreateOrUpdateWebhookResponse]       = Json.format
  implicit val getWebhooksReqFmt: OFormat[GetWebhooksRequest]                            = Json.format
  implicit val getWebhooksResFmt: OFormat[GetWebhooksResponse]                           = Json.format
  implicit val deleteWebhookReqFmt: OFormat[DeleteWebhookRequest]                        = Json.format
  implicit val deleteWebhookResFmt: OFormat[DeleteWebhookResponse]                       = Json.format
}
