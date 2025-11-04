package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class NewAppeal(

  val appealDate: java.time.LocalDate,

  val appealDetail: kotlin.String,

  val decision: AppealDecision,

  val decisionDetail: kotlin.String,
)
