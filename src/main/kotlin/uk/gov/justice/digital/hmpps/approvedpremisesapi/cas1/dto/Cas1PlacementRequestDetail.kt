package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1PlacementRequestDetail(

  val type: ApType,
  @Schema(example = "B74", required = true, description = "Postcode outcode")
  val location: String,
  val radius: Int,
  val essentialCriteria: List<PlacementCriteria>,
  val id: UUID,
  val person: Person,
  val risks: PersonRisks,
  val applicationId: UUID,
  val assessmentId: UUID,
  val releaseType: ReleaseTypeOption,
  val status: PlacementRequestStatus,
  val assessmentDecision: AssessmentDecision,
  val assessmentDate: Instant,
  val applicationDate: Instant,
  val assessor: ApprovedPremisesUser,
  val isParole: Boolean,
  val isWithdrawn: Boolean,
  val application: Cas1Application,
  val spaceBookings: List<Cas1SpaceBookingSummary>,
  @Deprecated("Use spaceBookings instead")
  val booking: PlacementRequestBookingSummary? = null,
  val openChangeRequests: List<Cas1ChangeRequestSummary>,
  @Schema(description = "Notes from the assessor for the CRU Manager")
  val notes: String? = null,
  val requestType: PlacementRequestRequestType? = null,
  val withdrawalReason: WithdrawPlacementRequestReason? = null,
  val legacyBooking: PlacementRequestBookingSummary? = null,
  val requestedPlacementPeriod: Cas1RequestedPlacementPeriod,
  val authorisedPlacementPeriod: Cas1RequestedPlacementPeriod,
  @Schema(deprecated = true, description = "Use Cas1RequestedPlacementPeriod instead")
  val expectedArrival: LocalDate,
  @Schema(deprecated = true, description = "Use Cas1RequestedPlacementPeriod instead")
  val duration: Int,
)
