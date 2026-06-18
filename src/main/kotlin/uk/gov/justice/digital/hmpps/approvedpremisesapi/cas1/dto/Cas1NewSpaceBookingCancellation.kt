package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

data class Cas1NewSpaceBookingCancellation(

  @get:JsonProperty("occurredAt", required = true) val occurredAt: LocalDate,

  @get:JsonProperty("reasonId", required = true) val reasonId: UUID,

  @get:JsonProperty("reasonNotes") val reasonNotes: String? = null,
)
