package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Cas2v2User(

  @get:JsonProperty("id", required = true) val id: UUID,

  @field:Schema(example = "Roger Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @field:Schema(example = "SMITHR_GEN", required = true, description = "")
  @get:JsonProperty("username", required = true) val username: String,

  @get:JsonProperty("authSource", required = true) val authSource: AuthSource,

  @field:Schema(example = "true", required = true, description = "")
  @get:JsonProperty("isActive", required = true) val isActive: Boolean,

  @field:Schema(example = "Roger.Smith@justice.gov.uk", description = "")
  @get:JsonProperty("email") val email: String? = null,
) {

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
