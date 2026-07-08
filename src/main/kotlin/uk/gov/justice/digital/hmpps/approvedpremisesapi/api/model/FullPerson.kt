package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class FullPerson(
  val name: String,
  val dateOfBirth: java.time.LocalDate,
  val sex: String,
  val status: PersonStatus,
  override val crn: String,
  override val type: PersonType,
  val nomsNumber: String? = null,
  val pncNumber: String? = null,
  val ethnicity: String? = null,
  val nationality: String? = null,
  val religionOrBelief: String? = null,
  val genderIdentity: String? = null,
  val prisonName: String? = null,
  val isRestricted: Boolean? = null,
) : Person
