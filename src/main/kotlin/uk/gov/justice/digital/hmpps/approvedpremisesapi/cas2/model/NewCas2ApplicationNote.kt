package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A note to add to an application
 * @param note
 */
data class NewCas2ApplicationNote(

  val note: String,
)
