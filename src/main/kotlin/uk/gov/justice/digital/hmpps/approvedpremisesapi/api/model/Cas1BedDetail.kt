package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1BedDetail(

  val id: java.util.UUID,

  val name: kotlin.String,

  val roomName: kotlin.String,

  val status: BedStatus,

  val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,
)
