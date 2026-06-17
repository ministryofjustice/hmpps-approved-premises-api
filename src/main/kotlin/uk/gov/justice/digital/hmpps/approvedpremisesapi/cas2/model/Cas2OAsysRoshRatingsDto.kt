package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Cas2OAsysRoshRatingsDto(
  val metadata: Cas2OASysAssessmentMetadataDto,
  val overallRisk: Cas2OASysRiskLevel?,
  val riskToChildren: Cas2OASysRiskLevel?,
  val riskToPublic: Cas2OASysRiskLevel?,
  val riskToKnownAdult: Cas2OASysRiskLevel?,
  val riskToStaff: Cas2OASysRiskLevel?,
)

enum class Cas2OASysRiskLevel(@get:JsonValue val value: String) {
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
