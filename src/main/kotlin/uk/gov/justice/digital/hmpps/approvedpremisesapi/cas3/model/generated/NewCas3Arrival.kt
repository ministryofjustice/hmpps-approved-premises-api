package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

data class NewCas3Arrival(

  val arrivalDate: java.time.LocalDate,

  val type: String,

  val expectedDepartureDate: java.time.LocalDate,

  val notes: String? = null,

  val keyWorkerStaffCode: String? = null,
)
