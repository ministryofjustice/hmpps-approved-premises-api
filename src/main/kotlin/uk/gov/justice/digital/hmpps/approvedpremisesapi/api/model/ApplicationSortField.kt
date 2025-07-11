package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: tier,createdAt,arrivalDate,releaseType
*/
enum class ApplicationSortField(@get:JsonValue val value: kotlin.String) {

  tier("tier"),
  createdAt("createdAt"),
  arrivalDate("arrivalDate"),
  releaseType("releaseType"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApplicationSortField = entries.first { it.value == value }
  }
}
