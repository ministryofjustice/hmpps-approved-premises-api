package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A note to add to an application
 * @param note
 */
data class NewCas2ApplicationNote(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("note", required = true) val note: kotlin.String,
)
