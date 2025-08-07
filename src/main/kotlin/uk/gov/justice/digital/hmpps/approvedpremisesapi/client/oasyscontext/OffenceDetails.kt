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
  val offenceAnalysis: String?,
  val othersInvolved: String?,
  val issueContributingToRisk: String?,
  val offenceMotivation: String?,
  val victimImpact: String?,
  val victimPerpetratorRel: String?,
  val victimInfo: String?,
  val patternOffending: String?,
  val acceptsResponsibility: String?,
)
