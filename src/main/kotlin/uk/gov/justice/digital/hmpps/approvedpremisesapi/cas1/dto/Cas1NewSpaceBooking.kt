package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import java.time.LocalDate
import java.util.UUID

data class Cas1NewSpaceBooking(

  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
  val premisesId: UUID,
  val characteristics: List<Cas1SpaceCharacteristic>? = null,
  val transferReason: TransferReason? = null,
  val additionalInformation: String? = null,
)
