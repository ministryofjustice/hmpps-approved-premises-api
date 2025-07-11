package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A member of prison or CAS2 staff
 * @param staffIdentifier
 * @param name
 * @param username
 * @param cas2StaffIdentifier
 * @param usertype
 */
data class Cas2StaffMember(

  @Schema(example = "1501234567", required = true, description = "")
  @get:JsonProperty("staffIdentifier", required = true) val staffIdentifier: kotlin.Long,

  @Schema(example = "John Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "SMITHJ_GEN", description = "")
  @get:JsonProperty("username") val username: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cas2StaffIdentifier") val cas2StaffIdentifier: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("usertype") val usertype: Cas2StaffMember.Usertype? = null,
) {

  /**
   *
   * Values: nomis,delius
   */
  enum class Usertype(@get:JsonValue val value: kotlin.String) {

    nomis("nomis"),
    delius("delius"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): Usertype = entries.first { it.value == value }
    }
  }
}
