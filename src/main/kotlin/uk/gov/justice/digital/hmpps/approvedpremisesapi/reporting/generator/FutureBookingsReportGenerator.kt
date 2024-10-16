package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.FutureBookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.FutureBookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toYesNo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

class FutureBookingsReportGenerator {
  fun convert(bookingData: FutureBookingsReportData, personInfo: PersonSummaryInfoResult): FutureBookingsReportRow {
    return FutureBookingsReportRow(
      bookingId = bookingData.bookingId,
      referralId = bookingData.referralId,
      referralDate = bookingData.referralDate?.toLocalDate(),
      personName = personInfo.tryGetDetails {
        val nameParts = listOf(it.name.forename) + it.name.middleNames + it.name.surname
        nameParts.joinToString(" ")
      },
      gender = when (personInfo.tryGetDetails { it.profile?.genderIdentity }) {
        "Prefer to self-describe" -> personInfo.tryGetDetails { it.profile?.selfDescribedGender }
        else -> personInfo.tryGetDetails { it.profile?.genderIdentity }
      }.toString(),
      ethnicity = personInfo.tryGetDetails { it.profile?.ethnicity },
      dateOfBirth = personInfo.tryGetDetails { it.dateOfBirth },
      riskOfSeriousHarm = bookingData.riskOfSeriousHarm,
      registeredSexOffender = bookingData.registeredSexOffender.toYesNo(),
      historyOfSexualOffence = bookingData.historyOfSexualOffence.toYesNo(),
      concerningSexualBehaviour = bookingData.concerningSexualBehaviour.toYesNo(),
      dutyToReferMade = bookingData.dutyToReferMade.toYesNo(),
      dateDutyToReferMade = bookingData.dateDutyToReferMade,
      dutyToReferLocalAuthorityAreaName = bookingData.dutyToReferLocalAuthorityAreaName,
      probationRegion = bookingData.probationRegionName,
      pdu = bookingData.pduName,
      localAuthority = bookingData.localAuthorityAreaName,
      addressLine1 = bookingData.addressLine1,
      postCode = bookingData.postCode,
      crn = bookingData.crn,
      sourceOfReferral = bookingData.referralEligibilityReason,
      prisonAtReferral = bookingData.prisonNameOnCreation,
      accommodationRequiredDate = bookingData.accommodationRequiredDate?.toLocalDate(),
      updatedAccommodationRequiredDate = bookingData.updatedAccommodationRequiredDate,
      bookingStatus = if (bookingData.confirmationId == null) "Provisional" else "Confirmed",
    )
  }

  private fun <V> PersonSummaryInfoResult.tryGetDetails(value: (CaseSummary) -> V): V? {
    return when (this) {
      is PersonSummaryInfoResult.Success.Full -> value(this.summary)
      else -> null
    }
  }
}
