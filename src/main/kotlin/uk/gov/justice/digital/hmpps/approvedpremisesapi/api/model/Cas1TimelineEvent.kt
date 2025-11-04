package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param id
 * @param occurredAt
 * @param content Timeline description. If a value is provided for 'payload', that should be instead be used to build a description
 * @param createdBySummary
 * @param payload
 * @param associatedUrls
 * @param triggerSource
 * @param schemaVersion
 */
data class Cas1TimelineEvent(

  val type: Cas1TimelineEventType,

  val id: kotlin.String,

  val occurredAt: java.time.Instant,

  @Schema(example = "null", description = "Timeline description. If a value is provided for 'payload', that should be instead be used to build a description")
  val content: kotlin.String? = null,

  val createdBySummary: UserSummary? = null,

  val payload: Cas1TimelineEventContentPayload? = null,

  val associatedUrls: kotlin.collections.List<Cas1TimelineEventAssociatedUrl>? = null,

  val triggerSource: Cas1TriggerSourceType? = null,

  val schemaVersion: kotlin.Int? = null,
)
