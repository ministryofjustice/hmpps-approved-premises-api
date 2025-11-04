package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class SeedRequest(

  val seedType: SeedFileType,

  val fileName: kotlin.String,
)
