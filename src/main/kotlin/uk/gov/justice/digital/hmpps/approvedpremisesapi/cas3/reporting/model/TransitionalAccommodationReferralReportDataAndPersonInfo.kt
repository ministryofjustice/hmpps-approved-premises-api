package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.TransitionalAccommodationReferralReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult

data class TransitionalAccommodationReferralReportDataAndPersonInfo(
  val referralReportData: TransitionalAccommodationReferralReportData,
  val personInfoResult: PersonSummaryInfoResult,
)
