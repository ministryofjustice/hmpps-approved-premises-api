package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext

import java.time.OffsetDateTime

class RoshRatings(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val rosh: RoshRatingsInner,
) : AssessmentInfo(
  assessmentId,
  assessmentType,
  dateCompleted,
  assessorSignedDate,
  initiationDate,
  assessmentStatus,
  superStatus,
  limitedAccessOffender,
)

data class RoshRatingsInner(
  val riskChildrenCommunity: RiskLevel?,
  val riskChildrenCustody: RiskLevel?,
  val riskPrisonersCustody: RiskLevel?,
  val riskStaffCustody: RiskLevel?,
  val riskStaffCommunity: RiskLevel?,
  val riskKnownAdultCustody: RiskLevel?,
  val riskKnownAdultCommunity: RiskLevel?,
  val riskPublicCustody: RiskLevel?,
  val riskPublicCommunity: RiskLevel?,
) {
  fun determineOverallRiskLevel(): RiskLevel {
    val allLevels = listOf(riskChildrenCommunity, riskPrisonersCustody, riskStaffCommunity, riskStaffCustody, riskKnownAdultCommunity, riskKnownAdultCustody, riskPublicCommunity, riskPublicCustody)

    if (allLevels.contains(RiskLevel.VERY_HIGH)) return RiskLevel.VERY_HIGH
    if (allLevels.contains(RiskLevel.HIGH)) return RiskLevel.HIGH
    if (allLevels.contains(RiskLevel.MEDIUM)) return RiskLevel.MEDIUM
    if (allLevels.contains(RiskLevel.LOW)) return RiskLevel.LOW

    throw RuntimeException("No RiskLevels found")
  }

  fun anyRisksAreNull() = riskChildrenCommunity == null || riskPublicCommunity == null || riskKnownAdultCommunity == null || riskStaffCommunity == null
}

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RiskLevel(val text: String) {
  VERY_HIGH("Very High"),
  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low"),
}
