package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason

data class Cas1SpaceBookingCancellation(

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

  @get:JsonProperty("recordedAt", required = true) val recordedAt: java.time.Instant,

  @get:JsonProperty("reason", required = true) val reason: CancellationReason,

  @get:JsonProperty("reason_notes") val reasonNotes: String? = null,
)
