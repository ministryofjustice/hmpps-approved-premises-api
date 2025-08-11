package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import java.time.LocalDate

data class NewCas3Arrival(

  val arrivalDate: LocalDate,

  val type: String,

  val expectedDepartureDate: LocalDate,

  val notes: String? = null,

  val keyWorkerStaffCode: String? = null,
)
