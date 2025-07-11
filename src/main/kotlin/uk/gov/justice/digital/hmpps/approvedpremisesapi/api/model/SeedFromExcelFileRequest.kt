package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType

/**
 *
 * @param seedType
 * @param fileName File within the pre-configured seed directory
 */
data class SeedFromExcelFileRequest(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("seedType", required = true) val seedType: SeedFromExcelFileType,

  @Schema(example = "null", required = true, description = "File within the pre-configured seed directory")
  @get:JsonProperty("fileName", required = true) val fileName: kotlin.String,
)
