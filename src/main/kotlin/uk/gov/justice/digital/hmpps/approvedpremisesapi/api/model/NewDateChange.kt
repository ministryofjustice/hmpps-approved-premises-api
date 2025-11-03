package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param newArrivalDate
 * @param newDepartureDate
 */
data class NewDateChange(

  @get:JsonProperty("newArrivalDate") val newArrivalDate: java.time.LocalDate? = null,

  @get:JsonProperty("newDepartureDate") val newDepartureDate: java.time.LocalDate? = null,
)
