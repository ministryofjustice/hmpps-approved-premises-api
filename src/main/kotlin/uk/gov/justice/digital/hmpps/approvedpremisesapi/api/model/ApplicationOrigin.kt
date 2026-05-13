package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * The Application Origin is a misnomer as it doesn't identify the 'origin' of the application
 * in all cases. It can be thought of as the application's type
 */
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApplicationOrigin(@get:JsonValue val value: String) {

  courtBail("courtBail"),
  prisonBail("prisonBail"),
  homeDetentionCurfew("homeDetentionCurfew"),
  other("other"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): ApplicationOrigin = values().first { it.value == value }
  }
}
