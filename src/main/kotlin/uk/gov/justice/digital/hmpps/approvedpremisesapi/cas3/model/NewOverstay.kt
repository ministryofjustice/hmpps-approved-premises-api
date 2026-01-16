package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.LocalDate

data class NewOverstay(
  val newDepartureDate: LocalDate,
  val isAuthorised: Boolean,
  val reason: String? = null,
)
