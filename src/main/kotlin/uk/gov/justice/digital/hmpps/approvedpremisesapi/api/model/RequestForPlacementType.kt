package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: manual,automatic
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RequestForPlacementType(@get:JsonValue val value: kotlin.String) {

  manual("manual"),
  automatic("automatic"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): RequestForPlacementType = entries.first { it.value == value }
  }
}
