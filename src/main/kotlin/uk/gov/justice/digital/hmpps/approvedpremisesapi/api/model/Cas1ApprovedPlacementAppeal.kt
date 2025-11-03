package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param occurredAt
 * @param placementAppealChangeRequestId
 * @param reasonNotes
 */
data class Cas1ApprovedPlacementAppeal(

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

  @get:JsonProperty("placementAppealChangeRequestId", required = true) val placementAppealChangeRequestId: java.util.UUID,

  @get:JsonProperty("reasonNotes") val reasonNotes: kotlin.String? = null,
)
