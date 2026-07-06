package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1SpaceBooking(
  val id: UUID,
  val applicationId: UUID,
  val person: Person,
  val premises: NamedId,
  val apArea: NamedId,
  val expectedArrivalDate: LocalDate,
  val expectedDepartureDate: LocalDate,
  val canonicalArrivalDate: LocalDate,
  val canonicalDepartureDate: LocalDate,
  val createdAt: Instant,
  val otherBookingsInPremisesForCrn: List<Cas1SpaceBookingDates>,
  val characteristics: List<Cas1SpaceCharacteristic>,
  val allowedActions: List<Cas1SpaceBookingAction>,
  @Schema(description = "Change requests were developed but never used", deprecated = true)
  @Deprecated(message = "Change requests were developed but never used")
  val openChangeRequests: List<Cas1ChangeRequestSummary>,
  val assessmentId: UUID? = null,
  @Schema(description = "Tier when the application was created")
  val tier: String? = null,
  val bookedBy: User? = null,
  @Deprecated(message = "Use placementRequestId")
  val requestForPlacementId: UUID? = null,
  val placementRequestId: UUID? = null,
  val placementRequestApType: ApType? = null,
  val actualArrivalDate: LocalDate? = null,
  @Deprecated(message = "Use actualArrivalDate")
  val actualArrivalDateOnly: LocalDate? = null,
  val actualArrivalTime: String? = null,
  val actualDepartureDate: LocalDate? = null,
  @Deprecated(message = "Use actualDepartureDate")
  val actualDepartureDateOnly: LocalDate? = null,
  val actualDepartureTime: String? = null,
  val departure: Cas1SpaceBookingDeparture? = null,
  val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,
  val cancellation: Cas1SpaceBookingCancellation? = null,
  val nonArrival: Cas1SpaceBookingNonArrival? = null,
  val deliusEventNumber: String? = null,
  val additionalInformation: String? = null,
  val transferReason: TransferReason? = null,
  val status: Cas1SpaceBookingStatus? = null,
)
