package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Cas1ExternalApplicationV2Dto(
  val uiUrl: String,
  val id: UUID,
  val status: ApprovedPremisesApplicationStatus,
  val createdAt: OffsetDateTime,
  val createdBy: Cas1StaffDto,
  val expiresAt: LocalDate?,
  @Schema(description = "Only populated once the application has been submitted")
  val submittedApplication: Cas1ExternalSubmittedApplicationDto?,
)

data class Cas1ExternalSubmittedApplicationDto(
  val submittedAt: OffsetDateTime,
  val assessment: Cas1ExternalAssessmentDto?,
  val upcomingRequestForPlacement: Cas1ExternalRequestForPlacementOutcomeDto?,
  val requestForPlacementHistory: List<Cas1ExternalRequestForPlacementOutcomeDto>,
)

data class Cas1ExternalRequestForPlacementOutcomeDto(
  val requestForPlacement: Cas1ExternalRequestForPlacementDtoV2,
  val placement: Cas1ExternalPlacementV2Dto?,
)

data class Cas1ExternalRequestForPlacementDtoV2(
  val status: RequestForPlacementStatus,
  val submittedBy: Cas1StaffDto?,
  val submittedAt: LocalDate?,
  @Schema(description = "The requested arrival date. This may be different to the booked arrival date (check placements for this)")
  val expectedArrivalDate: LocalDate?,
  val durationDays: Int?,
  val decision: PlacementApplicationDecisionDto?,
  val rejectionReason: String?,
  val withdrawal: Cas1ExternalRequestForPlacementWithdrawalDto?,
)

@Schema(description = "There is a small number of older withdrawn request for placements for which reason and/or date were not captured")
data class Cas1ExternalRequestForPlacementWithdrawalDto(
  val withdrawalReason: WithdrawPlacementRequestReason?,
  val withdrawalDate: LocalDate?,
)

data class Cas1ExternalPlacementV2Dto(
  val status: Cas1SpaceBookingStatus,
  val actualArrivalDate: LocalDate?,
  val actualDepartureDate: LocalDate?,
  val expectedArrivalDate: LocalDate,
  val expectedDepartureDate: LocalDate,
  val cancellationReason: String?,
  val premises: Cas1ExternalPremisesV2Dto,
)

data class Cas1ExternalPremisesV2Dto(
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
)

@Schema(description = "Details about the most current application, with associated assessment, request for placement and placement")
data class Cas1SuitableApplication(
  val uiUrl: String,
  val application: Cas1ExternalApplicationDto,
  val assessment: Cas1ExternalAssessmentDto?,
  @Deprecated("Use suitableRequestForPlacement.requestForPlacement")
  val requestForPlacement: Cas1ExternalRequestForPlacementDto?,
  @Deprecated("Use suitableRequestForPlacement.placement")
  val placement: Cas1ExternalPlacementDto?,
  val suitableRequestForPlacement: Cas1RequestForPlacementOutcomeDto?,
  val placementHistory: List<Cas1RequestForPlacementOutcomeDto>,
)

@Schema(
  description = """Details about a request for placement and, if defined, one of its placements. 
  |If an RfP has many placements, many of these instances will exists""",
)
data class Cas1RequestForPlacementOutcomeDto(
  val requestForPlacement: Cas1ExternalRequestForPlacementDto,
  val placement: Cas1ExternalPlacementDto?,
  @Schema(description = "The most recent status change date, or expected arrival date if placement exists in the future")
  val dateApplied: LocalDate,
  val statusLastUpdated: LocalDate,
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
  val expectedArrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate?,
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
