package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param workingDays
 */
data class NewTurnaround(

  @get:JsonProperty("workingDays", required = true) val workingDays: kotlin.Int,
)
