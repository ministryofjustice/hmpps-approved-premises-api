package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: normal,pipe,esap,rfap,mhapStJosephs,mhapElliottHouse
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApType(@get:JsonValue val value: kotlin.String) {

  normal("normal"),
  pipe("pipe"),
  esap("esap"),
  rfap("rfap"),
  mhapStJosephs("mhapStJosephs"),
  mhapElliottHouse("mhapElliottHouse"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApType = entries.first { it.value == value }
  }
}
