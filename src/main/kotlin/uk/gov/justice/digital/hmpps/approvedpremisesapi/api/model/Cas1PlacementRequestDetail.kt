package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod

data class Cas1PlacementRequestDetail(

  val type: ApType,
  @Schema(example = "B74", required = true, description = "Postcode outcode")
  val location: kotlin.String,
  val radius: kotlin.Int,
  val essentialCriteria: kotlin.collections.List<PlacementCriteria>,
  val desirableCriteria: kotlin.collections.List<PlacementCriteria>,
  val id: java.util.UUID,
  val person: Person,
  val risks: PersonRisks,
  val applicationId: java.util.UUID,
  val assessmentId: java.util.UUID,
  val releaseType: ReleaseTypeOption,
  val status: PlacementRequestStatus,
  val assessmentDecision: AssessmentDecision,
  val assessmentDate: java.time.Instant,
  val applicationDate: java.time.Instant,
  val assessor: ApprovedPremisesUser,
  val isParole: kotlin.Boolean,
  val isWithdrawn: kotlin.Boolean,
  val application: Cas1Application,
  val spaceBookings: kotlin.collections.List<Cas1SpaceBookingSummary>,
  val openChangeRequests: kotlin.collections.List<Cas1ChangeRequestSummary>,
  @Schema(description = "Notes from the assessor for the CRU Manager")
  val notes: kotlin.String? = null,
  val booking: PlacementRequestBookingSummary? = null,
  val requestType: PlacementRequestRequestType? = null,
  val withdrawalReason: WithdrawPlacementRequestReason? = null,
  val legacyBooking: PlacementRequestBookingSummary? = null,
  val requestedPlacementPeriod: Cas1RequestedPlacementPeriod,
  val authorisedPlacementPeriod: Cas1RequestedPlacementPeriod,
)
