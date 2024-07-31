package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import java.time.LocalDate

data class PersonInformationReportData(
  val pnc: String?,
  val name: Name?,
  val dateOfBirth: LocalDate?,
  val gender: String?,
  val ethnicity: String?,
)
