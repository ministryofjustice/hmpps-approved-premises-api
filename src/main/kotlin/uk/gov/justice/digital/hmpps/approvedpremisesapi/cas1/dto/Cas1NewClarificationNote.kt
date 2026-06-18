package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1NewClarificationNote(

  @get:JsonProperty("query", required = true) val query: String,
)
