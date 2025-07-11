package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: courtBail,prisonBail,homeDetentionCurfew
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApplicationOrigin(@get:JsonValue val value: kotlin.String) {

  courtBail("courtBail"),
  prisonBail("prisonBail"),
  homeDetentionCurfew("homeDetentionCurfew"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApplicationOrigin = values().first { it -> it.value == value }
  }
}
