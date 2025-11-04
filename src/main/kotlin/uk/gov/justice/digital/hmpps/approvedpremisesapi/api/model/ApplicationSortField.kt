package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApplicationSortField(@get:JsonValue val value: kotlin.String) {

  tier("tier"),
  createdAt("createdAt"),
  arrivalDate("arrivalDate"),
  releaseType("releaseType"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApplicationSortField = values().first { it -> it.value == value }
  }
}
