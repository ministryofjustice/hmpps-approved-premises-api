package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.LocalDate

data class Cas1PersonDetails(
  val name: String,
  val alias: String? = null,
  val dateOfBirth: LocalDate,
  val nationality: String? = null,
  val immigrationStatus: String? = null,
  val languages: String? = null,
  val relationshipStatus: String? = null,
  val dependants: String? = null,
  val disabilities: String? = null,
  val tier: String? = null,
  val nomsId: String? = null,
  val pnc: String? = null,
  val ethnicity: String? = null,
  val religion: String? = null,
  val sex: String? = null,
  val genderIdentity: String? = null,
  val sexualOrientation: String? = null,
)
