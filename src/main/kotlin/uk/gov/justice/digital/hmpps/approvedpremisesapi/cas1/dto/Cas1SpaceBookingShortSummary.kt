package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import java.time.LocalDate
import java.time.LocalDateTime
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
  val createdAt: LocalDateTime? = null,
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
  val statusSetDate: LocalDate? = null,
  val placementRequestApType: ApType? = null,
)
