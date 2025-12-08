package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.LocalDate

data class Cas1PersonDetails(
  val name: String,
  val dateOfBirth: LocalDate,
  val nationality: String? = null,
  val tier: String? = null,
  val nomsId: String? = null,
  val pnc: String? = null,
  val ethnicity: String? = null,
  val religion: String? = null,
  val genderIdentity: String? = null,
)
