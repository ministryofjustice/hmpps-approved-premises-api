package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class PlacementRequest(

  val type: ApType,

  @field:Schema(example = "B74", required = true, description = "Postcode outcode")
  val location: kotlin.String,

  val radius: kotlin.Int,

  val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

  val expectedArrival: java.time.LocalDate,

  val duration: kotlin.Int,

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

  @field:Schema(example = "null", description = "Notes from the assessor for the CRU Manager")
  val notes: kotlin.String? = null,

  val booking: PlacementRequestBookingSummary? = null,

  val requestType: PlacementRequestRequestType? = null,

  val withdrawalReason: WithdrawPlacementRequestReason? = null,
)
