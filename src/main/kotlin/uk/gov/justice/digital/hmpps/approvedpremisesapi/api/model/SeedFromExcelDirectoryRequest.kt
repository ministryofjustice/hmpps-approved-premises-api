package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param seedType
 * @param directoryName Directory within the pre-configured seed directory
 */
data class SeedFromExcelDirectoryRequest(

  val seedType: SeedFromExcelFileType,

  @Schema(example = "null", required = true, description = "Directory within the pre-configured seed directory")
  val directoryName: kotlin.String,
)
