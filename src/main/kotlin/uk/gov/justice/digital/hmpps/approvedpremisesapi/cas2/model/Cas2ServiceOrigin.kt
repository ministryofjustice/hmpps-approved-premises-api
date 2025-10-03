package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Cas2ServiceOrigin(@get:JsonValue val value: String) {

  BAIL("BAIL"),
  HDC("HDC"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas2ServiceOrigin = values().first { it.value == value }
  }
}
