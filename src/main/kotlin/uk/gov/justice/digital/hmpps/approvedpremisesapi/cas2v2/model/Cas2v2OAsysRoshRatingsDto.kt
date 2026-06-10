package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Cas2v2OAsysRoshRatingsDto(
  val metadata: Cas2v2OASysAssessmentMetadataDto,
  val overallRisk: Cas2V2OASysRiskLevel?,
  val riskToChildren: Cas2V2OASysRiskLevel?,
  val riskToPublic: Cas2V2OASysRiskLevel?,
  val riskToKnownAdult: Cas2V2OASysRiskLevel?,
  val riskToStaff: Cas2V2OASysRiskLevel?,
)

enum class Cas2V2OASysRiskLevel(@get:JsonValue val value: String) {
  VERY_HIGH("very_high"),
  HIGH("high"),
  MEDIUM("medium"),
  LOW("low"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String?) = entries.firstOrNull { it.value == value }
  }
}
