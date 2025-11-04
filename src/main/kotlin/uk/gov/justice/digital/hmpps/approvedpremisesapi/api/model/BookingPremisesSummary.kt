package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param name
 */
data class BookingPremisesSummary(

  val id: java.util.UUID,

  val name: kotlin.String,
)
