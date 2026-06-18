package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1SpaceBookingSummary(

  val id: UUID,

  val person: PersonSummary,

  val premises: NamedId,

  val canonicalArrivalDate: LocalDate,

  val canonicalDepartureDate: LocalDate,

  val expectedArrivalDate: LocalDate,

  val expectedDepartureDate: LocalDate,

  val characteristics: List<Cas1SpaceCharacteristic> = arrayListOf(),

  val isCancelled: Boolean,

  val openChangeRequestTypes: List<Cas1ChangeRequestType>,

  val actualArrivalDate: LocalDate? = null,

  val actualDepartureDate: LocalDate? = null,

  val isNonArrival: Boolean? = null,

  @Schema(description = "Tier when the application was created")
  val tier: String? = null,

  val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,

  val deliusEventNumber: String? = null,

  @Deprecated(message = "")
  val plannedTransferRequested: Boolean? = null,

  @Deprecated(message = "")
  val appealRequested: Boolean? = null,

  val createdAt: Instant? = null,

  val transferReason: TransferReason? = null,

  val additionalInformation: String? = null,
  val status: Cas1SpaceBookingStatus? = null,
)
