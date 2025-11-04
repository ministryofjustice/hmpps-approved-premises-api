package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param location Postcode outcode
 * @param radius
 * @param essentialCriteria
 * @param desirableCriteria
 * @param expectedArrival
 * @param duration
 * @param id
 * @param person
 * @param risks
 * @param applicationId
 * @param assessmentId
 * @param releaseType
 * @param status
 * @param assessmentDecision
 * @param assessmentDate
 * @param applicationDate
 * @param assessor
 * @param isParole
 * @param isWithdrawn
 * @param cancellations Not used by UI. Space Booking cancellations to be provided if cancellations are required in future.
 * @param application
 * @param spaceBookings The space bookings associated with this placement request
 * @param notes Notes from the assessor for the CRU Manager
 * @param booking
 * @param requestType
 * @param withdrawalReason
 * @param legacyBooking
 */
data class PlacementRequestDetail(

  val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  val location: kotlin.String,

  val radius: kotlin.Int,

  val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

  val desirableCriteria: kotlin.collections.List<PlacementCriteria>,

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

  @Schema(example = "null", required = true, description = "Not used by UI. Space Booking cancellations to be provided if cancellations are required in future.")
  val cancellations: kotlin.collections.List<Cancellation>,

  val application: Application,

  @Schema(example = "null", required = true, description = "The space bookings associated with this placement request")
  val spaceBookings: kotlin.collections.List<Cas1SpaceBookingSummary>,

  @Schema(example = "null", description = "Notes from the assessor for the CRU Manager")
  val notes: kotlin.String? = null,

  val booking: PlacementRequestBookingSummary? = null,

  val requestType: PlacementRequestRequestType? = null,

  val withdrawalReason: WithdrawPlacementRequestReason? = null,

  val legacyBooking: PlacementRequestBookingSummary? = null,
)
