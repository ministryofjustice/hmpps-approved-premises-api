package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param seedType
 * @param fileName
 */
data class SeedRequest(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("seedType", required = true) val seedType: SeedFileType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("fileName", required = true) val fileName: kotlin.String,
)
