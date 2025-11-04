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

  val type: TimelineEventType,

  val occurredAt: Instant,

  val label: String,

  val body: String? = null,

  val createdByName: String? = null,
)
