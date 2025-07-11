package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonCreator

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TemporaryAccommodationUserRole(val value: String) {

  assessor("assessor"),
  referrer("referrer"),
  reporter("reporter"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): TemporaryAccommodationUserRole = entries.first { it.value == value }
  }
}
