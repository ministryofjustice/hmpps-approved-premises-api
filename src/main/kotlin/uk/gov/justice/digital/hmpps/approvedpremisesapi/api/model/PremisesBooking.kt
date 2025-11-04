package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class PremisesBooking(

  val id: java.util.UUID? = null,

  val arrivalDate: java.time.LocalDate? = null,

  val departureDate: java.time.LocalDate? = null,

  val person: Person? = null,

  val bed: Bed? = null,

  val status: BookingStatus? = null,
)
