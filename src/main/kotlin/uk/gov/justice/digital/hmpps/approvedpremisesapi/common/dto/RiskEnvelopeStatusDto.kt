package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "RiskEnvelopeStatus")
enum class RiskEnvelopeStatusDto(@get:JsonValue val value: String) {

  retrieved("retrieved"),
  notFound("not_found"),
  error("error"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): RiskEnvelopeStatusDto = entries.first { it.value == value }
  }
}
