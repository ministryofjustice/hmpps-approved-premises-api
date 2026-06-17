package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

data class Cas2OAsysRiskToSelfDto(
  val metadata: Cas2OASysAssessmentMetadataDto,
  val analysisSuicideSelfharm: String?,
  val analysisVulnerabilities: String?,
)
