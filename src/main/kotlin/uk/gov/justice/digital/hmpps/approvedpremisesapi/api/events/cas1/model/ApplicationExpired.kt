package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

data class ApplicationExpired(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @get:JsonProperty("previousStatus", required = true) val previousStatus: kotlin.String,

  @get:JsonProperty("updatedStatus", required = true) val updatedStatus: kotlin.String,

  @Schema(example = "null", required = false, description = "The status of the application before expiry")
  @get:JsonProperty("statusBeforeExpiry") val statusBeforeExpiry: kotlin.String? = null,

  @Schema(example = "null", required = false, description = "The reason for the application's expiry")
  @get:JsonProperty("expiryReason") val expiryReason: ApplicationExpired.ExpiryReason = ExpiryReason.unsubmittedApplicationExpired,
) : Cas1DomainEventPayload {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class ExpiryReason(@get:JsonValue val value: kotlin.String) {

    assessmentExpired("AssessmentExpired"),
    unsubmittedApplicationExpired("UnsubmittedApplicationExpired"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): ApplicationExpired.ExpiryReason = ApplicationExpired.ExpiryReason.values().first { it -> it.value == value }
    }
  }
}
