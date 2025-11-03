package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A note to add to an application
 * @param note
 */
data class NewApplicationTimelineNote(

  @get:JsonProperty("note", required = true) val note: kotlin.String,
)
