package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class CancellationReason(

  val id: java.util.UUID,

  val name: kotlin.String,

  val isActive: kotlin.Boolean,

  val serviceScope: kotlin.String,
)
