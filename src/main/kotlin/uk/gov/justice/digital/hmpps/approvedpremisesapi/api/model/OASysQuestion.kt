package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class OASysQuestion(

  val label: kotlin.String,

  val questionNumber: kotlin.String,

  val answer: kotlin.String? = null,
)
