package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1SpaceBookingShortSummary(

  val id: java.util.UUID,

  val premises: NamedId,

  val apArea: NamedId,

  val deliusEventNumber: String? = null,

  val actualArrivalDate: java.time.LocalDate?,

  val actualDepartureDate: java.time.LocalDate?,

  val expectedArrivalDate: java.time.LocalDate,

  val expectedDepartureDate: java.time.LocalDate,

  val createdAt: java.time.LocalDateTime? = null,

  val isNonArrival: Boolean? = null,

  val cancellation: Cas1SpaceBookingCancellation? = null,

  val characteristics: List<Cas1SpaceCharacteristic> = emptyList(),

)
