package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "PlacementApplicationDecision")
enum class PlacementApplicationDecisionDto(@get:JsonValue val value: String) {

  accepted("accepted"),
  rejected("rejected"),
  withdraw("withdraw"),
  withdrawnByPp("withdrawn_by_pp"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): PlacementApplicationDecisionDto = entries.first { it.value == value }
  }
}
