package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewCas2v2ApplicationNote(

  @get:JsonProperty("note", required = true) val note: String,
)
