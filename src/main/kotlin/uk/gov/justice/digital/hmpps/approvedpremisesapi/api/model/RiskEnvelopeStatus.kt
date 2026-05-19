package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RiskEnvelopeStatus(@get:JsonValue val value: String) {

  retrieved("retrieved"),
  notFound("not_found"),
  error("error"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): RiskEnvelopeStatus = values().first { it -> it.value == value }
  }
}
