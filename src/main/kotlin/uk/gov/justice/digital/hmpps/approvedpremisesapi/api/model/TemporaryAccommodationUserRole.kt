package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: assessor,referrer,reporter
*/
enum class TemporaryAccommodationUserRole(@get:JsonValue val value: kotlin.String) {

  assessor("assessor"),
  referrer("referrer"),
  reporter("reporter"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TemporaryAccommodationUserRole = values().first { it -> it.value == value }
  }
}
