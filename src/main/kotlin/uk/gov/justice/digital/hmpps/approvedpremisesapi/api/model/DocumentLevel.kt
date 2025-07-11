package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* The level at which a Document is associated - i.e. to the Offender or to a specific Conviction
* Values: offender,conviction
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class DocumentLevel(@get:JsonValue val value: kotlin.String) {

  offender("Offender"),
  conviction("Conviction"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): DocumentLevel = entries.first { it.value == value }
  }
}
