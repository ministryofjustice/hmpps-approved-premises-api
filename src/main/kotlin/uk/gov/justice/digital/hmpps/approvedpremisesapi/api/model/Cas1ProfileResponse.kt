package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1ProfileResponse(

  @field:Schema(
    required = true,
    description = "The Delius username of the user. This field is mandatory.",
  )
  val deliusUsername: String,

  @field:Schema(
    example = "staff_record_not_found",
    description = """
        The potential error encountered while loading the profile,Null if no error occurred.
    """,
  )
  val loadError: Cas1LoadError? = null,

  @field:Schema(
    example = "null",
    description = "The user details. Null if there is an error or user details are unavailable.",
  )
  val user: ApprovedPremisesUser? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Cas1LoadError(@get:JsonValue val value: String) {

    @field:Schema(description = "The user's staff record was not found.")
    staffRecordNotFound("staff_record_not_found"),

    @field:Schema(description = "The probation region of the user is not supported.")
    unsupportedProbationRegion("unsupported_probation_region"),
  }
}
