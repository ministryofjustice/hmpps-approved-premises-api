package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class NewDateChange(

  val newArrivalDate: java.time.LocalDate? = null,

  val newDepartureDate: java.time.LocalDate? = null,
)
