package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import java.time.Instant

/**
 *
 * @param type
 * @param occurredAt
 * @param label
 * @param body
 * @param createdByName
 */
data class Cas2TimelineEvent(

  @get:JsonProperty("type", required = true) val type: TimelineEventType,

  @get:JsonProperty("occurredAt", required = true) val occurredAt: Instant,

  @get:JsonProperty("label", required = true) val label: String,

  @get:JsonProperty("body") val body: String? = null,

  @get:JsonProperty("createdByName") val createdByName: String? = null,
)
