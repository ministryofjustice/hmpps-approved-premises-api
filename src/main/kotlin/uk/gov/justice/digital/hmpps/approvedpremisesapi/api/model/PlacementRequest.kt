package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class PlacementRequest(

  val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  val location: String,

  val radius: Int,

  val essentialCriteria: List<PlacementCriteria>,

  val expectedArrival: java.time.LocalDate,

  val duration: Int,

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

  val isParole: Boolean,

  val isWithdrawn: Boolean,

  @Schema(example = "null", description = "Notes from the assessor for the CRU Manager")
  val notes: String? = null,

  val booking: PlacementRequestBookingSummary? = null,

  val requestType: PlacementRequestRequestType? = null,

  val withdrawalReason: WithdrawPlacementRequestReason? = null,
)
