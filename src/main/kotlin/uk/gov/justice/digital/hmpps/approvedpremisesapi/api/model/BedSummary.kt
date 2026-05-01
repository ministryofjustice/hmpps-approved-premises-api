package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BedSummary(

  val id: java.util.UUID,

  val name: kotlin.String,

  val roomName: kotlin.String,

  val status: BedStatus,
)
