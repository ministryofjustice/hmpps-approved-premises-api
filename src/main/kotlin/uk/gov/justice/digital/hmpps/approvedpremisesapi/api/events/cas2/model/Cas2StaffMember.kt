package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

data class Cas2StaffMember(

  @Schema(example = "1501234567", required = true, description = "")
  val staffIdentifier: kotlin.Long,

  @Schema(example = "John Smith", required = true, description = "")
  val name: kotlin.String,

  @Schema(example = "SMITHJ_GEN", description = "")
  val username: kotlin.String? = null,

  val cas2StaffIdentifier: kotlin.String? = null,

  val usertype: Cas2StaffMember.Usertype? = null,
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
