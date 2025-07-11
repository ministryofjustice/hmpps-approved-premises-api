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
 * @param application
 * @param spaceBookings The space bookings associated with this placement request
 * @param openChangeRequests
 * @param notes Notes from the assessor for the CRU Manager
 * @param booking
 * @param requestType
 * @param withdrawalReason
 * @param legacyBooking
 */
data class Cas1PlacementRequestDetail(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  @get:JsonProperty("location", required = true) val location: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("radius", required = true) val radius: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("essentialCriteria", required = true) val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("desirableCriteria", required = true) val desirableCriteria: kotlin.collections.List<PlacementCriteria>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("duration", required = true) val duration: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("risks", required = true) val risks: PersonRisks,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessmentId", required = true) val assessmentId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("releaseType", required = true) val releaseType: ReleaseTypeOption,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: PlacementRequestStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessmentDecision", required = true) val assessmentDecision: AssessmentDecision,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessmentDate", required = true) val assessmentDate: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationDate", required = true) val applicationDate: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessor", required = true) val assessor: ApprovedPremisesUser,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isParole", required = true) val isParole: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("application", required = true) val application: Cas1Application,

  @Schema(example = "null", required = true, description = "The space bookings associated with this placement request")
  @get:JsonProperty("spaceBookings", required = true) val spaceBookings: kotlin.collections.List<Cas1SpaceBookingSummary>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("openChangeRequests", required = true) val openChangeRequests: kotlin.collections.List<Cas1ChangeRequestSummary>,

  @Schema(example = "null", description = "Notes from the assessor for the CRU Manager")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("booking") val booking: PlacementRequestBookingSummary? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("requestType") val requestType: PlacementRequestRequestType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("withdrawalReason") val withdrawalReason: WithdrawPlacementRequestReason? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("legacyBooking") val legacyBooking: PlacementRequestBookingSummary? = null,
)
