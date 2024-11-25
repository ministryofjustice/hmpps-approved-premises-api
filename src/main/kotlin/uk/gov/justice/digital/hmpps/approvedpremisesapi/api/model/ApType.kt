package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: normal,pipe,esap,rfap,mhapStJosephs,mhapElliottHouse
*/
enum class ApType(val value: kotlin.String) {

  @JsonProperty("normal")
  normal("normal"),

  @JsonProperty("pipe")
  pipe("pipe"),

  @JsonProperty("esap")
  esap("esap"),

  @JsonProperty("rfap")
  rfap("rfap"),

  @JsonProperty("mhapStJosephs")
  mhapStJosephs("mhapStJosephs"),

  @JsonProperty("mhapElliottHouse")
  mhapElliottHouse("mhapElliottHouse"),
}
