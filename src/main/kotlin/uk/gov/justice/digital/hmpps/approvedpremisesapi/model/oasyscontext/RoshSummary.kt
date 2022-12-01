package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext

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
  val roshSummary: RoshSummaryInner?
) : AssessmentInfo(
  assessmentId,
  assessmentType,
  dateCompleted,
  assessorSignedDate,
  initiationDate,
  assessmentStatus,
  superStatus,
  limitedAccessOffender
)

data class RoshSummaryInner(
  val whoAtRisk: String?,
  val natureOfRisk: String?,
  val riskGreatest: String?,
  val riskIncreaseLikelyTo: String?,
  val riskReductionLikelyTo: String?
)
