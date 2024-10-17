package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.FutureBookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult

data class FutureBookingsReportDataAndPersonInfo(
  val futureBookingsReportData: FutureBookingsReportData,
  val personInfoResult: PersonSummaryInfoResult,
)
