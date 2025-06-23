package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext

import java.time.OffsetDateTime

@Suppress("LongParameterList")
class NeedsDetails(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val needs: NeedsDetailsInner?,
  val linksToHarm: LinksToHarm?,
  val linksToReOffending: LinksToReOffending?,
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

data class NeedsDetailsInner(
  val offenceAnalysisDetails: String?,
  val emotionalIssuesDetails: String?,
  val drugIssuesDetails: String?,
  val alcoholIssuesDetails: String?,
  val lifestyleIssuesDetails: String?,
  val relationshipIssuesDetails: String?,
  val financeIssuesDetails: String?,
  val educationTrainingEmploymentIssuesDetails: String?,
  val accommodationIssuesDetails: String?,
  val attitudeIssuesDetails: String?,
  val thinkingBehaviouralIssuesDetails: String?,
)

data class LinksToHarm(
  val accommodationLinkedToHarm: Boolean?,
  val educationTrainingEmploymentLinkedToHarm: Boolean?,
  val financeLinkedToHarm: Boolean?,
  val relationshipLinkedToHarm: Boolean?,
  val lifestyleLinkedToHarm: Boolean?,
  val drugLinkedToHarm: Boolean?,
  val alcoholLinkedToHarm: Boolean?,
  val emotionalLinkedToHarm: Boolean?,
  val thinkingBehaviouralLinkedToHarm: Boolean?,
  val attitudeLinkedToHarm: Boolean?,
)

data class LinksToReOffending(
  val accommodationLinkedToReOffending: Boolean?,
  val educationTrainingEmploymentLinkedToReOffending: Boolean?,
  val financeLinkedToReOffending: Boolean?,
  val relationshipLinkedToReOffending: Boolean?,
  val lifestyleLinkedToReOffending: Boolean?,
  val drugLinkedToReOffending: Boolean?,
  val alcoholLinkedToReOffending: Boolean?,
  val emotionalLinkedToReOffending: Boolean?,
  val thinkingBehaviouralLinkedToReOffending: Boolean?,
  val attitudeLinkedToReOffending: Boolean?,
)
