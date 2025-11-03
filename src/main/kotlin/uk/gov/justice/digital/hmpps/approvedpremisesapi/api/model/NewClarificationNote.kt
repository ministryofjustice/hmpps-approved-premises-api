package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param query
 */
data class NewClarificationNote(

  @get:JsonProperty("query", required = true) val query: kotlin.String,
)
