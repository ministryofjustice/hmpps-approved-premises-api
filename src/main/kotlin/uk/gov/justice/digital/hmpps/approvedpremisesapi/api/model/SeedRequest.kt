package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param seedType
 * @param fileName
 */
data class SeedRequest(

  @get:JsonProperty("seedType", required = true) val seedType: SeedFileType,

  @get:JsonProperty("fileName", required = true) val fileName: kotlin.String,
)
