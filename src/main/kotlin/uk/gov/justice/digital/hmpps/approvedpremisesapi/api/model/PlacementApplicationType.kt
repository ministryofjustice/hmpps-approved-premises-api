package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* 'Initial' means that the request for placement was created for the arrival date included on the original application.  'Additional' means the request for placement was created after the application had been assessed as suitable. A given application should only have, at most, one request for placement of type 'Initial'.
* Values: initial,additional
*/
enum class PlacementApplicationType(@get:JsonValue val value: kotlin.String) {

  initial("Initial"),
  additional("Additional"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementApplicationType = values().first { it -> it.value == value }
  }
}
