package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Withdrawables(

  val notes: kotlin.collections.List<kotlin.String>,

  val withdrawables: kotlin.collections.List<Withdrawable>,
)
