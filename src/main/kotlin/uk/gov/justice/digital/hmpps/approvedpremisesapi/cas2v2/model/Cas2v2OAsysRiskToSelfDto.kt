package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model

data class Cas2v2OAsysRiskToSelfDto(
  val metadata: Cas2v2OASysAssessmentMetadataDto,
  val analysisSuicideSelfharm: String?,
  val analysisVulnerabilities: String?,
)
