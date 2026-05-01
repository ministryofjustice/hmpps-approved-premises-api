package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class NewExtension(

  val newDepartureDate: java.time.LocalDate,

  val notes: kotlin.String? = null,
)
