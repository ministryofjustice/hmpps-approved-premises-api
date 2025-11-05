package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class SeedFromExcelDirectoryRequest(

  @get:JsonProperty("seedType", required = true) val seedType: SeedFromExcelFileType,

  @field:Schema(example = "null", required = true, description = "Directory within the pre-configured seed directory")
  @get:JsonProperty("directoryName", required = true) val directoryName: kotlin.String,
)
