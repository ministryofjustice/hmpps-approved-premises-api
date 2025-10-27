package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: assessor,referrer,reporter
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TemporaryAccommodationUserRole(@get:JsonValue val value: kotlin.String) {

  assessor("assessor"),
  referrer("referrer"),
  reporter("reporter"),
  admin("admin"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TemporaryAccommodationUserRole = values().first { it.value == value }
  }
}
