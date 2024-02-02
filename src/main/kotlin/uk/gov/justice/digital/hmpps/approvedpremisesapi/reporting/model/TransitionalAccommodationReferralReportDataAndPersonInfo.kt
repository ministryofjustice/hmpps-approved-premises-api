package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportData

data class TransitionalAccommodationReferralReportDataAndPersonInfo(
  val referralReportData: TransitionalAccommodationReferralReportData,
  val personInfoResult: PersonSummaryInfoResult,
)
