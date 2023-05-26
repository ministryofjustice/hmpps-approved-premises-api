package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDate

data class OffenderDetailSummary(
  val offenderId: Long,
  val title: String?,
  val firstName: String,
  val middleNames: List<String>?,
  val surname: String,
  val previousSurname: String?,
  val preferredName: String?,
  val dateOfBirth: LocalDate,
  val gender: String,
  val otherIds: OffenderIds,
  val offenderProfile: OffenderProfile,
  val softDeleted: Boolean?,
  val currentDisposal: String,
  val partitionArea: String?,
  val currentRestriction: Boolean,
  val currentExclusion: Boolean,
  val isActiveProbationManagedSentence: Boolean,
)

data class OffenderIds(
  val crn: String,
  val croNumber: String?,
  val immigrationNumber: String?,
  val mostRecentPrisonNumber: String?,
  val niNumber: String?,
  val nomsNumber: String?,
  val pncNumber: String?,
)

data class OffenderProfile(
  val ethnicity: String?,
  val nationality: String?,
  val secondaryNationality: String?,
  val notes: String?,
  val immigrationStatus: String?,
  val offenderLanguages: OffenderLanguages,
  val religion: String?,
  val sexualOrientation: String?,
  val offenderDetails: String?,
  val remandStatus: String?,
  val riskColour: String?,
  val disabilities: List<Disability>?,
  val genderIdentity: String?,
  val selfDescribedGender: String?,
)

data class OffenderLanguages(
  val primaryLanguage: String?,
  val otherLanguages: List<String>?,
  val languageConcerns: String?,
  val requiresInterpreter: Boolean?,
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
