package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param occurredAt
 * @param placementAppealChangeRequestId
 * @param reasonNotes
 */
data class Cas1ApprovedPlacementAppeal(

  val occurredAt: java.time.LocalDate,

  val placementAppealChangeRequestId: java.util.UUID,

  val reasonNotes: kotlin.String? = null,
)
