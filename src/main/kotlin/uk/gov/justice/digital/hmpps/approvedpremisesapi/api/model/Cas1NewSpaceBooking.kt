package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1NewSpaceBooking(

  val arrivalDate: java.time.LocalDate,
  val departureDate: java.time.LocalDate,
  val premisesId: java.util.UUID,
  val characteristics: List<Cas1SpaceCharacteristic>? = null,
  val additionalInformation: String? = null,
)
