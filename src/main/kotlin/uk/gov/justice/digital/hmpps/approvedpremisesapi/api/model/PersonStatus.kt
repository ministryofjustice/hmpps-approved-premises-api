package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: inCustody,inCommunity,unknown
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PersonStatus(@get:JsonValue val value: kotlin.String) {

  inCustody("InCustody"),
  inCommunity("InCommunity"),
  unknown("Unknown"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PersonStatus = values().first { it -> it.value == value }
  }
}
