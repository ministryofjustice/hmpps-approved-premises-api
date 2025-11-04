package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class DateChange(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val previousArrivalDate: java.time.LocalDate,

  val newArrivalDate: java.time.LocalDate,

  val previousDepartureDate: java.time.LocalDate,

  val newDepartureDate: java.time.LocalDate,

  val createdAt: java.time.Instant,
)
