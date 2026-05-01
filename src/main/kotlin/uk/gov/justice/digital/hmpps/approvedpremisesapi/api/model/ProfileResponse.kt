package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue

data class ProfileResponse(

  val deliusUsername: kotlin.String,

  val loadError: ProfileResponse.LoadError? = null,

  val user: User? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class LoadError(@get:JsonValue val value: kotlin.String) {

    staffRecordNotFound("staff_record_not_found"),
  }
}
