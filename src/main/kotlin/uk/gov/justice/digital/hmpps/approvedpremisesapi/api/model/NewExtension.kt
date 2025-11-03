package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param newDepartureDate
 * @param notes
 */
data class NewExtension(

  @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: java.time.LocalDate,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
