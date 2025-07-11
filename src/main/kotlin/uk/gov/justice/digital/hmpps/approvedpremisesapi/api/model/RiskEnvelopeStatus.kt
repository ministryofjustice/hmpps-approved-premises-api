package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: retrieved,notFound,error
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RiskEnvelopeStatus(@get:JsonValue val value: kotlin.String) {

  retrieved("retrieved"),
  notFound("not_found"),
  error("error"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): RiskEnvelopeStatus = entries.first { it.value == value }
  }
}
