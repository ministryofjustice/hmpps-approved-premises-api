package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class NewCancellation(

  val date: java.time.LocalDate,

  val reason: java.util.UUID,

  val notes: kotlin.String? = null,

  val otherReason: kotlin.String? = null,
)
