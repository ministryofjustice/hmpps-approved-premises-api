package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: standardDeterminate,life,ipp,extendedDeterminate,communityOrder,bailPlacement,nonStatutory
*/
enum class SentenceTypeOption(@get:JsonValue val value: kotlin.String) {

  standardDeterminate("standardDeterminate"),
  life("life"),
  ipp("ipp"),
  extendedDeterminate("extendedDeterminate"),
  communityOrder("communityOrder"),
  bailPlacement("bailPlacement"),
  nonStatutory("nonStatutory"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SentenceTypeOption = values().first { it -> it.value == value }
  }
}
