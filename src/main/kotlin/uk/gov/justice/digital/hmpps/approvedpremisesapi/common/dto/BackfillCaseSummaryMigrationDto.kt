package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto

data class BackfillCaseSummaryMigrationDto(
  val crn: String,
  val name: String?,
  val nomsNumber: String?,
)
