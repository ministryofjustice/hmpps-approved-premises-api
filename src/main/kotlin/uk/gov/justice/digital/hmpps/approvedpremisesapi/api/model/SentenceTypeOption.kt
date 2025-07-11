package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: standardDeterminate,life,ipp,extendedDeterminate,communityOrder,bailPlacement,nonStatutory
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
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
    fun forValue(value: kotlin.String): SentenceTypeOption = entries.first { it.value == value }
  }
}
