package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys

import java.time.OffsetDateTime

class RoshSummary(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val roshSummary: RoshSummaryInner?,
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

data class RoshSummaryInner(
  // R10.1
  val whoIsAtRisk: String?,
  // R10.2
  val natureOfRisk: String?,
  // R10.3
  val riskGreatest: String?,
  // R10.4
  val riskIncreaseLikelyTo: String?,
  // R10.5
  val riskReductionLikelyTo: String?,
  // SUM9
  val factorsAnalysisOfRisk: String?,
  // SUM10
  val factorsStrengthsAndProtective: String?,
  // SUM11
  val factorsSituationsLikelyToOffend: String?,
)
