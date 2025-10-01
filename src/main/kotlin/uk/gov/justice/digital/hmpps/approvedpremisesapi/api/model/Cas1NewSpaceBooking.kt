package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param arrivalDate
 * @param departureDate
 * @param premisesId
 * @param requirements
 * @param characteristics
 */
data class Cas1NewSpaceBooking(

  val arrivalDate: java.time.LocalDate,
  val departureDate: java.time.LocalDate,
  val premisesId: java.util.UUID,
  val characteristics: List<Cas1SpaceCharacteristic>? = null,
  val reason: String? = null,
)
