package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: standardDeterminate,life,ipp,extendedDeterminate,communityOrder,bailPlacement,nonStatutory
*/
enum class SentenceTypeOption(val value: kotlin.String) {

  @JsonProperty("standardDeterminate")
  standardDeterminate("standardDeterminate"),

  @JsonProperty("life")
  life("life"),

  @JsonProperty("ipp")
  ipp("ipp"),

  @JsonProperty("extendedDeterminate")
  extendedDeterminate("extendedDeterminate"),

  @JsonProperty("communityOrder")
  communityOrder("communityOrder"),

  @JsonProperty("bailPlacement")
  bailPlacement("bailPlacement"),

  @JsonProperty("nonStatutory")
  nonStatutory("nonStatutory"),
}
