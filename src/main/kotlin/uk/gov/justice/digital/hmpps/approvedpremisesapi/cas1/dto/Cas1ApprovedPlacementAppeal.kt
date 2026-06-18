package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

data class Cas1ApprovedPlacementAppeal(

  @get:JsonProperty("occurredAt", required = true) val occurredAt: LocalDate,

  @get:JsonProperty("placementAppealChangeRequestId", required = true) val placementAppealChangeRequestId: UUID,

  @get:JsonProperty("reasonNotes") val reasonNotes: String? = null,
)
