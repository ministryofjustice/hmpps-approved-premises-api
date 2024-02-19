package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties

class TransitionalAccommodationReferralReportGenerator : ReportGenerator<
  TransitionalAccommodationReferralReportDataAndPersonInfo,
  TransitionalAccommodationReferralReportRow, TransitionalAccommodationReferralReportProperties,
  >(
  TransitionalAccommodationReferralReportRow::class,
) {

  override val convert: TransitionalAccommodationReferralReportDataAndPersonInfo.(properties: TransitionalAccommodationReferralReportProperties) -> List<TransitionalAccommodationReferralReportRow> = {
    val referralData = this.referralReportData
    val personInfo = this.personInfoResult

    listOf(
      TransitionalAccommodationReferralReportRow(
        referralId = referralData.referralId,
        referralDate = referralData.referralCreatedDate,
        personName = personInfo.tryGetDetails {
          val nameParts = listOf(it.name.forename) + it.name.middleNames + it.name.surname
          nameParts.joinToString(" ")
        },
        pncNumber = personInfo.tryGetDetails { it.pnc },
        crn = referralData.crn,
        gender = personInfo.tryGetDetails { it.gender },
        ethnicity = personInfo.tryGetDetails { it.profile?.ethnicity },
        dateOfBirth = personInfo.tryGetDetails { it.dateOfBirth },
        riskOfSeriousHarm = referralData.riskOfSeriousHarm,
        sexOffender = referralData.sexOffender,
        needForAccessibleProperty = referralData.needForAccessibleProperty,
        historyOfArsonOffence = referralData.historyOfArsonOffence,
        dutyToReferMade = referralData.dutyToReferMade,
        dateDutyToReferMade = referralData.dateDutyToReferMade,
        dutyToReferLocalAuthorityAreaName = referralData.dutyToReferLocalAuthorityAreaName,
        probationRegion = referralData.probationRegionName,
        referralSubmittedDate = referralData.referralSubmittedDate,
        referralRejected = AssessmentDecision.REJECTED.name == referralData.assessmentDecision,
        rejectionReason = referralData.assessmentRejectionReason,
        rejectionDate = if (AssessmentDecision.REJECTED.name == referralData.assessmentDecision) referralData.assessmentSubmittedDate else null,
        sourceOfReferral = referralData.referralEligibilityReason,
        prisonAtReferral = referralData.prisonNameOnCreation,
        prisonReleaseDate = null,
        accommodationRequiredDate = referralData.accommodationRequiredDate?.toLocalDateTime()?.toLocalDate(),
        bookingOffered = referralData.bookingId != null,
      ),
    )
  }

  override fun filter(properties: TransitionalAccommodationReferralReportProperties): (TransitionalAccommodationReferralReportDataAndPersonInfo) -> Boolean = {
    true
  }

  private fun<V> PersonSummaryInfoResult.tryGetDetails(value: (CaseSummary) -> V): V? {
    return when (this) {
      is PersonSummaryInfoResult.Success.Full -> value(this.summary)
      else -> null
    }
  }
}
