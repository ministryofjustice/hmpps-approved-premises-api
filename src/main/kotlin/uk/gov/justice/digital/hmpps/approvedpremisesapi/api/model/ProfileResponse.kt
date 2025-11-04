package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param deliusUsername
 * @param loadError
 * @param user
 */
data class ProfileResponse(

  @get:JsonProperty("deliusUsername", required = true) val deliusUsername: kotlin.String,

  @get:JsonProperty("loadError") val loadError: ProfileResponse.LoadError? = null,

  @get:JsonProperty("user") val user: User? = null,
) {

  /**
   *
   * Values: staffRecordNotFound
   */
  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class LoadError(@get:JsonValue val value: kotlin.String) {

    staffRecordNotFound("staff_record_not_found"),
  }
}
