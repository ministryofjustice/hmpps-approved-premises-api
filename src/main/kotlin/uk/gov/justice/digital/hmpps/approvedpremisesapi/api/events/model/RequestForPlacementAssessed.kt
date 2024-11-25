package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param placementApplicationId The UUID of a placement application
 * @param assessedBy
 * @param decision
 * @param expectedArrival
 * @param duration
 * @param decisionSummary
 */
data class RequestForPlacementAssessed(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of a placement application")
  @get:JsonProperty("placementApplicationId", required = true) val placementApplicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessedBy", required = true) val assessedBy: StaffMember,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("decision", required = true) val decision: RequestForPlacementAssessed.Decision,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
  @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

  @Schema(example = "7", required = true, description = "")
  @get:JsonProperty("duration", required = true) val duration: kotlin.Int,

  @Schema(example = "the decision was to accept", description = "")
  @get:JsonProperty("decisionSummary") val decisionSummary: kotlin.String? = null,
) {

  /**
   *
   * Values: accepted,rejected
   */
  enum class Decision(val value: kotlin.String) {

    @JsonProperty("accepted")
    accepted("accepted"),

    @JsonProperty("rejected")
    rejected("rejected"),
  }
}
