package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param newDepartureDate
 * @param notes
 */
data class NewExtension(

  val newDepartureDate: java.time.LocalDate,

  val notes: kotlin.String? = null,
)
