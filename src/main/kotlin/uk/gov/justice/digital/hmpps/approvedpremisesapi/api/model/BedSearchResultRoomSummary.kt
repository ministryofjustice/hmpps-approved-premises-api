package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class BedSearchResultRoomSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("characteristics", required = true) val characteristics: List<CharacteristicPair>,
)
