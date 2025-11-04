package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class FullPerson(

  val name: kotlin.String,

  val dateOfBirth: java.time.LocalDate,

  val sex: kotlin.String,

  val status: PersonStatus,

  override val crn: kotlin.String,

  override val type: PersonType,

  val nomsNumber: kotlin.String? = null,

  val pncNumber: kotlin.String? = null,

  val ethnicity: kotlin.String? = null,

  val nationality: kotlin.String? = null,

  val religionOrBelief: kotlin.String? = null,

  val genderIdentity: kotlin.String? = null,

  val prisonName: kotlin.String? = null,

  val isRestricted: kotlin.Boolean? = null,
) : Person
