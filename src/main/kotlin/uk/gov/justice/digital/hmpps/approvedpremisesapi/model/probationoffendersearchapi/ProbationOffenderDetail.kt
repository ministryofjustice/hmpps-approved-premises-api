package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi

import java.time.LocalDate

data class ProbationOffenderDetail(
  val previousSurname: String? = null,
  val offenderId: Long,
  val title: String? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val otherIds: IDs,
  val contactDetails: ContactDetails? = null,
  val offenderProfile: OffenderProfile? = null,
  val softDeleted: Boolean? = null,
  val currentDisposal: String? = null,
  val partitionArea: String? = null,
  val currentRestriction: Boolean? = null,
  val restrictionMessage: String? = null,
  val currentExclusion: Boolean? = null,
  val exclusionMessage: String? = null,
  val highlight: Map<String, List<String>>? = null,
  val accessDenied: Boolean? = null,
  val currentTier: String? = null,
  val activeProbationManagedSentence: Boolean? = null,
)

data class IDs(
  val crn: String,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null,
  val previousCrn: String? = null,
)

data class ContactDetails(
  val phoneNumbers: List<PhoneNumber>? = null,
  val emailAddresses: List<String>? = null,
  val allowSMS: Boolean? = null,
)

data class PhoneNumber(
  val number: String? = null,
  val type: PhoneTypes? = null,
) {

  enum class PhoneTypes {
    TELEPHONE, MOBILE
  }
}

data class OffenderProfile(
  val ethnicity: String? = null,
  val nationality: String? = null,
  val secondaryNationality: String? = null,
  val notes: String? = null,
  val immigrationStatus: String? = null,
  val religion: String? = null,
  val sexualOrientation: String? = null,
  val offenderDetails: String? = null,
  val remandStatus: String? = null,
  val riskColour: String? = null,
)
