package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.LocalDate
import java.util.UUID

data class Cas1SpaceBookingShortSummary(

  val id: UUID,
  val premises: NamedId,
  val apArea: NamedId,
  val deliusEventNumber: String? = null,
  val actualArrivalDate: LocalDate?,
  val actualDepartureDate: LocalDate?,
  val expectedArrivalDate: LocalDate,
  val expectedDepartureDate: LocalDate,
  val createdAt: java.time.LocalDateTime? = null,
  val isNonArrival: Boolean? = null,
  val cancellation: Cas1SpaceBookingCancellation? = null,
  val characteristics: List<Cas1SpaceCharacteristic> = emptyList(),
  val bookedBy: User? = null,
  val departure: Cas1SpaceBookingDeparture? = null,
  val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,
  val nonArrival: Cas1SpaceBookingNonArrival? = null,
  val additionalInformation: String? = null,
  val transferReason: TransferReason? = null,
  val status: Cas1SpaceBookingStatus? = null,
)
