package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDate

data class OffenderDetailSummary(
  val title: String? = null,
  val firstName: String,
  val middleNames: List<String>?,
  val surname: String,
  val previousSurname: String? = null,
  val preferredName: String? = null,
  val dateOfBirth: LocalDate,
  val gender: String,
  val otherIds: OffenderIds,
  val offenderProfile: OffenderProfile,
  val softDeleted: Boolean?,
  val currentRestriction: Boolean,
  val currentExclusion: Boolean,
)

data class OffenderIds(
  val crn: String,
  val croNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val pncNumber: String? = null,
)

data class OffenderProfile(
  val ethnicity: String?,
  val nationality: String?,
  val secondaryNationality: String? = null,
  val notes: String? = null,
  val immigrationStatus: String? = null,
  val offenderLanguages: OffenderLanguages,
  val religion: String? = null,
  val sexualOrientation: String? = null,
  val offenderDetails: String? = null,
  val remandStatus: String? = null,
  val riskColour: String? = null,
  val disabilities: List<Disability>? = null,
  val genderIdentity: String?,
  val selfDescribedGender: String? = null,
)

data class OffenderLanguages(
  val primaryLanguage: String? = null,
  val otherLanguages: List<String>? = null,
  val languageConcerns: String? = null,
  val requiresInterpreter: Boolean? = null,
)

data class Disability(
  val disabilityId: Long?,
  val disabilityType: DisabilityType?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val notes: String?,
  val provisions: List<DisabilityProvision>?,
  val lastUpdatedDateTime: LocalDate?,
  val isActive: Boolean,
)

data class DisabilityType(
  val code: String?,
  val description: String?,
)

data class DisabilityProvision(
  val provisionId: Long,
  val notes: String?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val provisionType: ProvisionType?,
)

data class ProvisionType(
  val code: String?,
  val description: String?,
)
