package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param confirmedAt
 * @param reason
 * @param notes
 */
data class Cas1SpaceBookingNonArrival(

  @get:JsonProperty("confirmedAt") val confirmedAt: java.time.Instant? = null,

  @get:JsonProperty("reason") val reason: NamedId? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
