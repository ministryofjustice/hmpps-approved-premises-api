package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class SeedFromExcelDirectoryRequest(

  val seedType: SeedFromExcelFileType,

  @Schema(example = "null", required = true, description = "Directory within the pre-configured seed directory")
  val directoryName: kotlin.String,
)
