package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param seedType
 * @param fileName File within the pre-configured seed directory
 */
data class SeedFromExcelFileRequest(

  val seedType: SeedFromExcelFileType,

  @Schema(example = "null", required = true, description = "File within the pre-configured seed directory")
  val fileName: kotlin.String,
)
