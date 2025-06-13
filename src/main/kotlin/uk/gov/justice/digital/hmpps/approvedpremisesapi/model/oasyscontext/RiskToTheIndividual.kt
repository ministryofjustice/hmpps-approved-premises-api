package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext

import java.time.OffsetDateTime

class RisksToTheIndividual(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val riskToTheIndividual: RiskToTheIndividualInner?,
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

data class RiskToTheIndividualInner(
  val currentConcernsSelfHarmSuicide: String?,
  val previousConcernsSelfHarmSuicide: String?,
  val currentCustodyHostelCoping: String?,
  val previousCustodyHostelCoping: String?,
  val currentVulnerability: String?,
  val previousVulnerability: String?,
  val riskOfSeriousHarm: String?,
  val currentConcernsBreachOfTrustText: String?,
  val analysisSuicideSelfharm: String?,
  val analysisCoping: String?,
  val analysisVulnerabilities: String?,
)
