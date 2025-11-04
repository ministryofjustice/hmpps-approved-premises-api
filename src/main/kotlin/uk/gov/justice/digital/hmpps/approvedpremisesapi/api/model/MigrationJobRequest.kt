package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param jobType
 */
data class MigrationJobRequest(

  val jobType: MigrationJobType,
)
