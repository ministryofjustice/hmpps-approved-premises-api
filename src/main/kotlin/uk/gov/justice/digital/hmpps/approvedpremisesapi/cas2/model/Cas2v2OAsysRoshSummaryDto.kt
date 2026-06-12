package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

data class Cas2v2OAsysRoshSummaryDto(
  val metadata: Cas2v2OASysAssessmentMetadataDto,
  val whoIsAtRisk: String?,
  val natureOfRisk: String?,
)
