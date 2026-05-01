package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BedSearchResultRoomSummary(

  val id: java.util.UUID,

  val name: kotlin.String,

  val characteristics: kotlin.collections.List<CharacteristicPair>,
)
