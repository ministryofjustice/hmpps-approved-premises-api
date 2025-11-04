package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param reason
 * @param parentReason
 * @param moveOnCategory
 * @param notes
 */
data class Cas1SpaceBookingDeparture(

  val reason: NamedId,

  val parentReason: NamedId? = null,

  val moveOnCategory: NamedId? = null,

  val notes: kotlin.String? = null,
)
