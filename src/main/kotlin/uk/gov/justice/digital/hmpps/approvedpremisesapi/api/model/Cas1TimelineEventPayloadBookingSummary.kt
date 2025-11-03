package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1TimelineEventPayloadBookingSummary(

  val bookingId: java.util.UUID,

  val premises: NamedId,

  val arrivalDate: java.time.LocalDate,

  val departureDate: java.time.LocalDate,

  val transferReason: TransferReason? = null,

  val additionalInformation: String? = null,
)
