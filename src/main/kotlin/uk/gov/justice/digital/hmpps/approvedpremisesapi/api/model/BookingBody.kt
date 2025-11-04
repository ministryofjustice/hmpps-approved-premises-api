package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BookingBody(

  val id: java.util.UUID,

  val person: Person,

  val arrivalDate: java.time.LocalDate,

  val originalArrivalDate: java.time.LocalDate,

  val departureDate: java.time.LocalDate,

  val originalDepartureDate: java.time.LocalDate,

  val createdAt: java.time.Instant,

  val serviceName: ServiceName,

  val bed: Bed? = null,
)
