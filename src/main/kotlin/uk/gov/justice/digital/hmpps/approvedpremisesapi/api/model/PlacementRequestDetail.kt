package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementRequestDetail(

  @get:JsonProperty("type", required = true) val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  @get:JsonProperty("location", required = true) val location: kotlin.String,

  @get:JsonProperty("radius", required = true) val radius: kotlin.Int,

  @get:JsonProperty("essentialCriteria", required = true) val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

  @get:JsonProperty("desirableCriteria", required = true) val desirableCriteria: kotlin.collections.List<PlacementCriteria>,

  @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

  @get:JsonProperty("duration", required = true) val duration: kotlin.Int,

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("risks", required = true) val risks: PersonRisks,

  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @get:JsonProperty("assessmentId", required = true) val assessmentId: java.util.UUID,

  @get:JsonProperty("releaseType", required = true) val releaseType: ReleaseTypeOption,

  @get:JsonProperty("status", required = true) val status: PlacementRequestStatus,

  @get:JsonProperty("assessmentDecision", required = true) val assessmentDecision: AssessmentDecision,

  @get:JsonProperty("assessmentDate", required = true) val assessmentDate: java.time.Instant,

  @get:JsonProperty("applicationDate", required = true) val applicationDate: java.time.Instant,

  @get:JsonProperty("assessor", required = true) val assessor: ApprovedPremisesUser,

  @get:JsonProperty("isParole", required = true) val isParole: kotlin.Boolean,

  @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "Not used by UI. Space Booking cancellations to be provided if cancellations are required in future.")
  @get:JsonProperty("cancellations", required = true) val cancellations: kotlin.collections.List<Cancellation>,

  @get:JsonProperty("application", required = true) val application: Application,

  @Schema(example = "null", required = true, description = "The space bookings associated with this placement request")
  @get:JsonProperty("spaceBookings", required = true) val spaceBookings: kotlin.collections.List<Cas1SpaceBookingSummary>,

  @Schema(example = "null", description = "Notes from the assessor for the CRU Manager")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("booking") val booking: PlacementRequestBookingSummary? = null,

  @get:JsonProperty("requestType") val requestType: PlacementRequestRequestType? = null,

  @get:JsonProperty("withdrawalReason") val withdrawalReason: WithdrawPlacementRequestReason? = null,

  @get:JsonProperty("legacyBooking") val legacyBooking: PlacementRequestBookingSummary? = null,
)
