package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.LocalDate

data class Cas3UpdateAssessment(
  val releaseDate: LocalDate? = null,
  val accommodationRequiredFromDate: LocalDate? = null,
)
