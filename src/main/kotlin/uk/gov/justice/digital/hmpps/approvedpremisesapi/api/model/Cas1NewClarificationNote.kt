package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1NewClarificationNote(

  @get:JsonProperty("query", required = true) val query: kotlin.String,
)
