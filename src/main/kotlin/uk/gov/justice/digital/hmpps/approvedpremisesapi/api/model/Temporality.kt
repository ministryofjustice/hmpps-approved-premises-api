package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: past,current,future
*/
enum class Temporality(@get:JsonValue val value: kotlin.String) {

  past("past"),
  current("current"),
  future("future"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Temporality = values().first { it -> it.value == value }
  }
}
