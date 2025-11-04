package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param bedId
 * @param notes
 */
data class NewBedMove(

  val bedId: java.util.UUID,

  val notes: kotlin.String? = null,
)
