package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.FutureBookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult

data class FutureBookingsReportDataAndPersonInfo(
  val futureBookingsReportData: FutureBookingsReportData,
  val personInfoResult: PersonSummaryInfoResult,
)
