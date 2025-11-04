package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param seedType
 * @param fileName
 */
data class SeedRequest(

  val seedType: SeedFileType,

  val fileName: kotlin.String,
)
