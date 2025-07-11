package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param deliusUsername
 * @param loadError
 * @param user
 */
data class ProfileResponse(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("deliusUsername", required = true) val deliusUsername: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("loadError") val loadError: ProfileResponse.LoadError? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("user") val user: User? = null,
) {

  /**
   *
   * Values: staffRecordNotFound
   */
  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class LoadError(@get:JsonValue val value: kotlin.String) {

    staffRecordNotFound("staff_record_not_found"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): LoadError = entries.first { it.value == value }
    }
  }
}
