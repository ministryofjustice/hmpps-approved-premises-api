package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext

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
  // R8.1.1
  val currentConcernsSelfHarmSuicide: String?,
  // R8.1.4
  val previousConcernsSelfHarmSuicide: String?,
  // R8.2.1
  val currentCustodyHostelCoping: String?,
  val previousCustodyHostelCoping: String?,
  // R8.3.1
  val currentVulnerability: String?,
  val previousVulnerability: String?,
  val riskOfSeriousHarm: String?,
  val currentConcernsBreachOfTrustText: String?,
  // FA62 (appears as 8.1 in oasys)
  val analysisSuicideSelfharm: String?,
  // FA63 (appears as 8.2 in oasys)
  val analysisCoping: String?,
  // FA64 (appears as 8.3 in oasys)
  val analysisVulnerabilities: String?,
)
