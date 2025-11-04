package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param name
 * @param characteristics
 */
data class BedSearchResultRoomSummary(

  val id: java.util.UUID,

  val name: kotlin.String,

  val characteristics: kotlin.collections.List<CharacteristicPair>,
)
