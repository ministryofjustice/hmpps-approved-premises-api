package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class OASysSupportingInformationQuestion(

  val label: kotlin.String,

  val questionNumber: kotlin.String,

  val sectionNumber: kotlin.Int? = null,

  val linkedToHarm: kotlin.Boolean? = null,

  val linkedToReOffending: kotlin.Boolean? = null,

  val answer: kotlin.String? = null,
)
