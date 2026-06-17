package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewCas2ApplicationNote(

  @get:JsonProperty("note", required = true) val note: String,
)
