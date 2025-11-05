package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

data class Cas2StaffMember(

  @field:Schema(example = "1501234567", required = true, description = "")
  @get:JsonProperty("staffIdentifier", required = true) val staffIdentifier: kotlin.Long,

  @field:Schema(example = "John Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @field:Schema(example = "SMITHJ_GEN", description = "")
  @get:JsonProperty("username") val username: kotlin.String? = null,

  @get:JsonProperty("cas2StaffIdentifier") val cas2StaffIdentifier: kotlin.String? = null,

  @get:JsonProperty("usertype") val usertype: Cas2StaffMember.Usertype? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Usertype(@get:JsonValue val value: kotlin.String) {

    nomis("nomis"),
    delius("delius"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): Usertype = values().first { it -> it.value == value }
    }
  }
}
