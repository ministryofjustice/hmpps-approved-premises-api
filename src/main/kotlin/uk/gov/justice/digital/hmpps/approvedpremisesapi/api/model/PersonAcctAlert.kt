package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class PersonAcctAlert(

  val alertId: kotlin.Long,

  val dateCreated: java.time.LocalDate,

  val comment: kotlin.String? = null,

  val description: kotlin.String? = null,

  val dateExpires: java.time.LocalDate? = null,

  val alertTypeDescription: kotlin.String? = null,
)
