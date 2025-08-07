package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppsauth

import com.fasterxml.jackson.annotation.JsonProperty

data class Jwt(
  val jti: String,
  val sub: String,
  val name: String?,
  val authorities: List<String>,
  @JsonProperty("auth_source")
  val authSource: String,
  @JsonProperty("user_id")
  val userId: String?,
  @JsonProperty("passed_mfa")
  val passedMfa: Boolean?,
  val exp: Long,
)
