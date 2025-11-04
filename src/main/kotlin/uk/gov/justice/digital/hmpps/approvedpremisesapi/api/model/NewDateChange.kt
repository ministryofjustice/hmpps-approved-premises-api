package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param newArrivalDate
 * @param newDepartureDate
 */
data class NewDateChange(

  val newArrivalDate: java.time.LocalDate? = null,

  val newDepartureDate: java.time.LocalDate? = null,
)
