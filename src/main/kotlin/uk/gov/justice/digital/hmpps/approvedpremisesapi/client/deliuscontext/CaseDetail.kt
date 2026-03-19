package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.time.ZonedDateTime

data class CaseDetail(
  val case: CaseSummary,
  val offences: List<Offence>,
  val registrations: List<Registration>,
  val mappaDetail: MappaDetail?,
  val sentences: List<Sentence>,
  val personalContacts: List<PersonalContact> = emptyList(),
)

data class CaseSummary(
  val crn: String,
  val nomsId: String?,
  val pnc: String?,
  val name: Name,
  val dateOfBirth: LocalDate,
  val gender: String?,
  val profile: Profile?,
  val manager: Manager,
  val currentExclusion: Boolean,
  val currentRestriction: Boolean,
)

data class Name(
  val forename: String,
  val surname: String,
  val middleNames: List<String> = emptyList(),
)

data class Manager(
  val team: Team,
)

data class Team(
  val code: String,
  val name: String,
  val ldu: Ldu,
  val borough: Borough?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
)

data class Borough(
  val code: String,
  val description: String,
)

data class Ldu(
  val code: String,
  val name: String,
)

data class Profile(
  val ethnicity: String? = null,
  val genderIdentity: String? = null,
  @Deprecated("This is not currently populated by ap-delius-context")
  val selfDescribedGender: String? = null,
  val nationality: String? = null,
  val religion: String? = null,
)

data class Offence(
  val id: String,
  val description: String,
  val mainCategoryDescription: String,
  val subCategoryDescription: String,
  val date: LocalDate?,
  val main: Boolean,
  val eventNumber: String,
  val eventId: Long,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Registration(
  val code: String,
  val description: String,
  val startDate: LocalDate,
  val riskNotes: String? = null,
  // The parser will populate this if the registration has a risk note
  val riskNotesDetail: List<NoteDetail> = emptyList(),
  val riskFlagGroupDescription: String? = null,
)

data class NoteDetail(
  val note: String,
  val date: LocalDate?,
)

data class MappaDetail(
  val level: Int?,
  val levelDescription: String?,
  val category: Int?,
  val categoryDescription: String?,
  val startDate: LocalDate,
  val lastUpdated: ZonedDateTime,
)

data class CaseSummaries(
  var cases: List<CaseSummary>,
)

data class ReferralDetail(
  /*
  Note that the time element is typically 00:00
   */
  val arrivedAt: ZonedDateTime?,
  /*
  Note that the time element is typically 00:00
   */
  val departedAt: ZonedDateTime?,
)

data class Sentence(
  val typeDescription: String?,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val eventNumber: String? = null,
)

data class PersonalContact(
  val relationship: String,
  val relationshipType: RelationshipType,
  val name: Name,
  val telephoneNumber: String?,
  val mobileNumber: String?,
  val address: Address?,
)

data class RelationshipType(
  val code: String,
  val description: String,
)

data class Address(
  val buildingName: String?,
  val addressNumber: String?,
  val streetName: String?,
  val district: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
)
