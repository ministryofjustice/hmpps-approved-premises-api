package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppsauth

import com.fasterxml.jackson.annotation.JsonProperty

data class GetTokenResponse(
  @JsonProperty("access_token")
  val accessToken: String,
  @JsonProperty("token_type")
  val tokenType: String,
  @JsonProperty("expires_in")
  val expiresIn: Int,
  val scope: String,
  val sub: String,
  @JsonProperty("auth_source")
  val authSource: String,
  val jti: String,
  val iss: String,
)
