package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TemporaryAccommodationUserRole(val value: String) {

  assessor("assessor"),
  referrer("referrer"),
  reporter("reporter"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): TemporaryAccommodationUserRole = values().first { it -> it.value == value }
  }
}
