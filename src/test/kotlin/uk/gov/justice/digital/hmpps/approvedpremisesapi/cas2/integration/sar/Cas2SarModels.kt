package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.DomainEventMetadata

// --- CAS2 (Short Term Accommodation) & CAS2v2 (Bail Accommodation) ---

data class Cas2SarData(
  @JsonProperty("Applications") val applications: List<Cas2Application>? = null,
  @JsonProperty("ApplicationNotes") val applicationNotes: List<Cas2ApplicationNote>? = null,
  @JsonProperty("Assessments") val assessments: List<Cas2Assessment>? = null,
  @JsonProperty("StatusUpdates") val statusUpdates: List<Cas2StatusUpdate>? = null,
  @JsonProperty("StatusUpdateDetails") val statusUpdateDetails: List<Cas2StatusUpdateDetail>? = null,
  @JsonProperty("DomainEvents") val domainEvents: List<DomainEvent>? = null,
  @JsonProperty("DomainEventsMetadata") val domainEventsMetadata: List<DomainEventMetadata>? = null,
)

data class Cas2v2SarData(
  @JsonProperty("Applications") val applications: List<Cas2Application>? = null,
  @JsonProperty("ApplicationNotes") val applicationNotes: List<Cas2ApplicationNote>? = null,
  @JsonProperty("Assessments") val assessments: List<Cas2Assessment>? = null,
  @JsonProperty("StatusUpdates") val statusUpdates: List<Cas2StatusUpdate>? = null,
  @JsonProperty("StatusUpdateDetails") val statusUpdateDetails: List<Cas2StatusUpdateDetail>? = null,
  @JsonProperty("DomainEvents") val domainEvents: List<DomainEvent>? = null,
  @JsonProperty("DomainEventsMetadata") val domainEventsMetadata: List<DomainEventMetadata>? = null,
)

data class Cas2Application(
  @JsonProperty("submitted_at") val submittedAt: String? = null,
  @JsonProperty("conditional_release_date") val conditionalReleaseDate: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("abandoned_at") val abandonedAt: String? = null,
  @JsonProperty("referring_prison_code") val referringPrisonCode: String? = null,
  @JsonProperty("preferred_areas") val preferredAreas: String? = null,
  @JsonProperty("hdc_eligibility_date") val hdcEligibilityDate: String? = null,
  val data: Cas2ApplicationData? = null,
)

data class Cas2ApplicationData(
  @JsonProperty("address-history") val addressHistory: AddressHistory? = null,
  @JsonProperty("offending-history") val offendingHistory: OffendingHistory? = null,
  @JsonProperty("risk-to-self") val riskToSelf: RiskToSelf? = null,
  @JsonProperty("cpp-details-and-hdc-licence-conditions") val cppDetailsAndHdcLicenceConditions: CppDetailsAndHdcLicenceConditions? = null,
  @JsonProperty("information-needed-from-applicant") val informationNeededFromApplicant: InformationNeededFromApplicant? = null,
  @JsonProperty("confirm-consent") val confirmConsent: ConfirmConsent? = null,
  @JsonProperty("funding-information") val fundingInformation: FundingInformation? = null,
  @JsonProperty("confirm-eligibility") val confirmEligibility: ConfirmEligibility? = null,
  @JsonProperty("current-offences") val currentOffences: CurrentOffences? = null,
  @JsonProperty("risk-information") val riskInformation: Map<String, Any>? = null,
  @JsonProperty("health-needs") val healthNeeds: Map<String, Any>? = null,
  @JsonProperty("prison-information") val prisonInformation: Map<String, Any>? = null,
  @JsonProperty("personal-information") val personalInformation: Map<String, Any>? = null,
)

data class InformationNeededFromApplicant(
  @JsonProperty("information-needed-from-applicant") val info: InformationNeeded? = null,
)

data class InformationNeeded(
  val hasInformationNeeded: String? = null,
)

data class ConfirmConsent(
  @JsonProperty("confirm-consent") val consent: ConsentDetail? = null,
)

data class ConsentDetail(
  val hasGivenConsent: String? = null,
  val consentDate: String? = null,
)

data class FundingInformation(
  @JsonProperty("national-insurance") val nationalInsurance: NationalInsurance? = null,
  @JsonProperty("funding-source") val fundingSource: FundingSource? = null,
)

data class NationalInsurance(
  val nationalInsuranceNumber: String? = null,
)

data class FundingSource(
  val fundingSource: String? = null,
)

data class ConfirmEligibility(
  @JsonProperty("confirm-eligibility") val eligibility: EligibilityDetail? = null,
)

data class EligibilityDetail(
  val isEligible: String? = null,
)

data class CurrentOffences(
  @JsonProperty("current-offence-data") val currentOffenceData: List<CurrentOffenceItem>? = null,
)

data class CurrentOffenceItem(
  val titleAndNumber: String? = null,
  @JsonProperty("offenceDate-year") val offenceDateYear: String? = null,
  @JsonProperty("offenceDate-month") val offenceDateMonth: String? = null,
  @JsonProperty("offenceDate-day") val offenceDateDay: String? = null,
  val offenceCategory: String? = null,
  val sentenceLength: String? = null,
  val summary: String? = null,
  val outstandingCharges: String? = null,
  val outstandingChargesDetail: String? = null,
)

data class CppDetailsAndHdcLicenceConditions(
  @JsonProperty("cpp-details") val cppDetails: CppDetails? = null,
  @JsonProperty("non-standard-licence-conditions") val nonStandardLicenceConditions: NonStandardLicenceConditions? = null,
)

data class CppDetails(
  val name: String? = null,
  val probationRegion: String? = null,
  val telephone: String? = null,
  val email: String? = null,
)

data class NonStandardLicenceConditions(
  val nonStandardLicenceConditions: String? = null,
  val nonStandardLicenceConditionsDetail: String? = null,
)

data class AddressHistory(
  @JsonProperty("previous-address") val previousAddress: Cas2PreviousAddress? = null,
)

data class Cas2PreviousAddress(
  val previousAddressLine1: String? = null,
  val previousAddressLine2: String? = null,
  val previousTownOrCity: String? = null,
  val previousCounty: String? = null,
  val previousPostcode: String? = null,
)

data class OffendingHistory(
  @JsonProperty("any-previous-convictions") val anyPreviousConvictions: AnyPreviousConvictions? = null,
  @JsonProperty("offence-history-data") val offenceHistoryData: List<OffenceHistoryItem>? = null,
)

data class AnyPreviousConvictions(val hasAnyPreviousConvictions: String? = null)

data class OffenceHistoryItem(
  val titleAndNumber: String? = null,
  @JsonProperty("offenceDate-year") val offenceDateYear: String? = null,
  @JsonProperty("offenceDate-month") val offenceDateMonth: String? = null,
  @JsonProperty("offenceDate-day") val offenceDateDay: String? = null,
  val offenceCategory: String? = null,
  val sentenceLength: String? = null,
  val summary: String? = null,
)

data class RiskToSelf(
  @JsonProperty("historical-risk") val historicalRisk: Cas2HistoricalRisk? = null,
  @JsonProperty("additional-information") val additionalInformation: Cas2AdditionalInformation? = null,
)

data class Cas2HistoricalRisk(
  val historicalRiskDetail: String? = null,
  val confirmation: String? = null,
)

data class Cas2AdditionalInformation(
  val hasAdditionalInformation: String? = null,
  val additionalInformationDetail: String? = null,
)

data class Cas2ApplicationNote(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("created_by_user") val createdByUser: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  val body: String? = null,
)

data class Cas2Assessment(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("allocated_at") val allocatedAt: String? = null,
)

data class Cas2StatusUpdate(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("status_label") val statusLabel: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
)

data class Cas2StatusUpdateDetail(
  val id: String? = null,
  @JsonProperty("status_update_id") val statusUpdateId: String? = null,
  @JsonProperty("status_label") val statusLabel: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("detail_label") val detailLabel: String? = null,
)
