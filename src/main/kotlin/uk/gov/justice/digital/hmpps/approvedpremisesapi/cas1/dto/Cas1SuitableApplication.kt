package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Details about the most current application, with associated assessment, request for placement and placement")
data class Cas1SuitableApplication(
  val uiUrl: String,
  val application: Cas1ExternalApplicationDto,
  val assessment: Cas1ExternalAssessmentDto?,
  val requestForPlacement: Cas1ExternalRequestForPlacementDto?,
  val placement: Cas1ExternalPlacementDto?,
  val placementHistory: List<Cas1PlacementPairDto>,

  @Deprecated("This field will be removed once SAS is updated to use application.id")
  val id: UUID,

  @Deprecated("This field will be removed once SAS is updated to use application.status")
  val applicationStatus: ApprovedPremisesApplicationStatus,

  @Deprecated("This field will be removed once SAS is updated to use requestForPlacement.status")
  val requestForPlacementStatus: RequestForPlacementStatus?,

  @Deprecated("This field will be removed once SAS is updated to use placement.status")
  val placementStatus: Cas1SpaceBookingStatus?,

  @Deprecated("This field will be removed once SAS is updated to use placement.premises")
  val premises: Cas1ExternalPremisesDto?,
)

@Schema(description = "Details about a placement, with it's associated request for placement")
data class Cas1PlacementPairDto(
  val requestForPlacement: Cas1ExternalRequestForPlacementDto?,
  val placement: Cas1ExternalPlacementDto?,
  val dateApplied: LocalDate,
)

data class Cas1ExternalApplicationDto(
  val id: UUID,
  val status: ApprovedPremisesApplicationStatus,
  val createdAt: OffsetDateTime,
  val createdBy: Cas1StaffDto,
  val submittedAt: OffsetDateTime?,
  val expiresAt: LocalDate?,
)

data class Cas1ExternalAssessmentDto(
  val decision: AssessmentDecision?,
  val rejectionRationale: String?,
)

data class Cas1ExternalRequestForPlacementDto(
  val status: RequestForPlacementStatus?,
  val decision: PlacementApplicationDecisionDto?,
  val rejectionReason: String?,
  val submittedBy: Cas1StaffDto?,
  val submittedAt: LocalDate?,
  val withdrawalReason: WithdrawPlacementRequestReason?,
  val withdrawalDate: LocalDate?,
  val expectedArrivalDate: LocalDate?,
  val durationDays: Int?,
)

data class Cas1ExternalPlacementDto(
  val status: Cas1SpaceBookingStatus?,
  val actualArrivalDate: LocalDate?,
  val actualDepartureDate: LocalDate?,
  val cancellationReason: String?,
  val premises: Cas1ExternalPremisesDto?,
)

data class Cas1ExternalPremisesDto(
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
)
