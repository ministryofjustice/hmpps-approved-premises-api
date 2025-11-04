package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param username
 * @param authSource
 * @param isActive
 * @param email
 */
data class Cas2v2User(

  val id: UUID,

  @Schema(example = "Roger Smith", required = true, description = "")
  val name: String,

  @Schema(example = "SMITHR_GEN", required = true, description = "")
  val username: String,

  val authSource: AuthSource,

  @Schema(example = "true", required = true, description = "")
  val isActive: Boolean,

  @Schema(example = "Roger.Smith@justice.gov.uk", description = "")
  val email: String? = null,
) {

  /**
   *
   * Values: nomis,delius,auth
   */
  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class AuthSource(@get:JsonValue val value: String) {

    nomis("nomis"),
    delius("delius"),
    auth("auth"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: String): AuthSource = values().first { it -> it.value == value }
    }
  }
}
