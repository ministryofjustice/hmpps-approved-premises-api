package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param personReference
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param releaseType
 * @param age
 * @param gender
 * @param targetLocation
 * @param submittedAt
 * @param submittedBy
 * @param mappa
 * @param sentenceLengthInMonths
 * @param offenceId
 */
data class ApplicationSubmitted(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @Schema(example = "rotl", required = true, description = "")
  @get:JsonProperty("releaseType", required = true) val releaseType: kotlin.String,

  @Schema(example = "43", required = true, description = "")
  @get:JsonProperty("age", required = true) val age: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("gender", required = true) val gender: ApplicationSubmitted.Gender,

  @Schema(example = "LS2", required = true, description = "")
  @get:JsonProperty("targetLocation", required = true) val targetLocation: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("submittedAt", required = true) val submittedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("submittedBy", required = true) val submittedBy: ApplicationSubmittedSubmittedBy,

  @Schema(example = "CAT C3/LEVEL L2", description = "")
  @get:JsonProperty("mappa") val mappa: kotlin.String? = null,

  @Schema(example = "57", description = "")
  @get:JsonProperty("sentenceLengthInMonths") val sentenceLengthInMonths: kotlin.Int? = null,

  @Schema(example = "AB43782", description = "")
  @get:JsonProperty("offenceId") val offenceId: kotlin.String? = null,
) {

  /**
   *
   * Values: male,female
   */
  enum class Gender(val value: kotlin.String) {

    @JsonProperty("Male")
    male("Male"),

    @JsonProperty("Female")
    female("Female"),
  }
}
