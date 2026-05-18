package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext

import java.time.OffsetDateTime

class OffenceDetails(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val offence: OffenceDetailsInner?,
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

data class OffenceDetailsInner(
  // 2.1
  val offenceAnalysis: String?,
  val othersInvolved: String?,
  // 2.98
  val issueContributingToRisk: String?,
  // 2.8.3
  val offenceMotivation: String?,
  // 2.5
  val victimImpact: String?,
  // 2.4.1
  val victimPerpetratorRel: String?,
  // 2.4.2
  val victimInfo: String?,
  // 2.12
  val patternOffending: String?,
  val acceptsResponsibility: String?,
)
