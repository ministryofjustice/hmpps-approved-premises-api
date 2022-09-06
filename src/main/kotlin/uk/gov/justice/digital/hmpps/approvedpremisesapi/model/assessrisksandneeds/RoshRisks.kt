package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds

import java.time.LocalDateTime

data class RoshRisks(
  val riskToSelf: RoshRiskToSelf,
  val otherRisks: OtherRoshRisks,
  val summary: RoshRisksSummary,
  val assessedOn: LocalDateTime?
)

data class RoshRiskToSelf(
  val suicide: Risk?,
  val custody: Risk?,
  val hostelSetting: Risk?,
  val vulnerability: Risk?,
  val assessedOn: LocalDateTime?
)

data class OtherRoshRisks(
  val escapeOrAbscond: Response?,
  val controlIssuesDisruptiveBehaviour: Response?,
  val breachOfTrust: Response?,
  val riskToOtherPrisoners: Response?,
  val assessedOn: LocalDateTime?
)

data class RoshRisksSummary(
  val whoIsAtRisk: String?,
  val natureOfRisk: String?,
  val riskImminence: String?,
  val riskIncreaseFactors: String?,
  val riskMitigationFactors: String?,
  val riskInCommunity: Map<RiskLevel?, List<String>>,
  val riskInCustody: Map<RiskLevel?, List<String>>,
  val assessedOn: LocalDateTime?,
  val overallRiskLevel: RiskLevel? = null
)

data class Risk(
  val risk: Response?,
  val previous: Response?,
  val previousConcernsText: String?,
  val current: Response?,
  val currentConcernsText: String?
)

enum class Response(val value: String) {
  YES("Yes"),
  NO("No"),
  DK("Don't know")
}

enum class RiskLevel(val value: String) {
  VERY_HIGH("Very High"),
  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low")
}
