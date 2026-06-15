package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
@Deprecated("This is legacy functionality and we should look to remove it - see FM-740.")
enum class ServiceName(@get:JsonValue val value: String) {

  approvedPremises("approved-premises"),
  cas2("cas2"),
  cas2v2("cas2v2"),
  temporaryAccommodation("temporary-accommodation"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): ServiceName = values().first { it.value == value }
  }
}
