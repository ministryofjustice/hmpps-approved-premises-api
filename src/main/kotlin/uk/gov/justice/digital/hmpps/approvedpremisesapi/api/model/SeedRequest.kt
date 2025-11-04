package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param seedType
 * @param fileName
 */
data class SeedRequest(

  val seedType: SeedFileType,

  val fileName: kotlin.String,
)
