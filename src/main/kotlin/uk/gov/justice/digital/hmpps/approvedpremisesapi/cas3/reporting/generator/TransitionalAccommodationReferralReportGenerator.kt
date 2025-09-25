package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.getPersonGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.getPersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toYesNo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails

class TransitionalAccommodationReferralReportGenerator :
  ReportGenerator<
    TransitionalAccommodationReferralReportDataAndPersonInfo,
    TransitionalAccommodationReferralReportRow,
    TransitionalAccommodationReferralReportProperties,
    >(
    TransitionalAccommodationReferralReportRow::class,
  ) {

  override val convert: TransitionalAccommodationReferralReportDataAndPersonInfo.(properties: TransitionalAccommodationReferralReportProperties) -> List<TransitionalAccommodationReferralReportRow> =
    {
      val referralData = this.referralReportData
      val personInfo = this.personInfoResult

      listOf(
        TransitionalAccommodationReferralReportRow(
          referralId = referralData.referralId,
          referralDate = referralData.referralCreatedDate.toLocalDate(),
          personName = personInfo.getPersonName(),
          pncNumber = personInfo.tryGetDetails { it.pnc },
          crn = referralData.crn,
          sex = personInfo.tryGetDetails { it.gender },
          genderIdentity = personInfo.getPersonGender(),
          ethnicity = personInfo.tryGetDetails { it.profile?.ethnicity },
          dateOfBirth = personInfo.tryGetDetails { it.dateOfBirth },
          riskOfSeriousHarm = referralData.riskOfSeriousHarm,
          registeredSexOffender = referralData.registeredSexOffender.toYesNo(),
          historyOfSexualOffence = referralData.historyOfSexualOffence.toYesNo(),
          concerningSexualBehaviour = referralData.concerningSexualBehaviour.toYesNo(),
          needForAccessibleProperty = referralData.needForAccessibleProperty.toYesNo(),
          historyOfArsonOffence = referralData.historyOfArsonOffence.toYesNo(),
          concerningArsonBehaviour = referralData.concerningArsonBehaviour.toYesNo(),
          dutyToReferMade = referralData.dutyToReferMade.toYesNo(),
          dateDutyToReferMade = referralData.dateDutyToReferMade,
          dutyToReferLocalAuthorityAreaName = referralData.dutyToReferLocalAuthorityAreaName,
          dutyToReferOutcome = referralData.dutyToReferOutcome,
          town = referralData.town,
          postCode = referralData.postCode,
          probationRegion = when (referralData.previousReferralProbationRegionName) {
            null -> referralData.probationRegionName
            else -> referralData.previousReferralProbationRegionName!!
          },
          pdu = when (referralData.previousReferralPduName) {
            null -> referralData.pduName
            else -> referralData.previousReferralPduName
          },
          outOfRegion = when (referralData.previousReferralProbationRegionName) {
            null -> "No"
            else -> "Yes"
          },
          destinationRegion = referralData.previousReferralProbationRegionName?.let { referralData.probationRegionName },
          destinationPdu = referralData.previousReferralPduName?.let { referralData.pduName },
          referralSubmittedDate = referralData.referralSubmittedDate?.toLocalDate(),
          referralRejected = (referralData.referralRejectionReason != null).toYesNo(),
          rejectionReason = referralData.referralRejectionReason,
          rejectionReasonExplained = referralData.referralRejectionReasonDetail,
          rejectionDate = if (AssessmentDecision.REJECTED.name == referralData.assessmentDecision) referralData.assessmentSubmittedDate?.toLocalDate() else null,
          sourceOfReferral = referralData.referralEligibilityReason,
          prisonReleaseType = referralData.prisonReleaseTypes,
          prisonAtReferral = referralData.prisonNameOnCreation,
          releaseDate = referralData.personReleaseDate,
          updatedReleaseDate = referralData.updatedReleaseDate,
          accommodationRequiredDate = referralData.accommodationRequiredDate?.toLocalDate(),
          updatedAccommodationRequiredFromDate = referralData.updatedAccommodationRequiredFromDate,
          bookingOffered = (referralData.bookingId != null).toYesNo(),
        ),
      )
    }

  override fun filter(properties: TransitionalAccommodationReferralReportProperties): (TransitionalAccommodationReferralReportDataAndPersonInfo) -> Boolean = {
    true
  }
}
