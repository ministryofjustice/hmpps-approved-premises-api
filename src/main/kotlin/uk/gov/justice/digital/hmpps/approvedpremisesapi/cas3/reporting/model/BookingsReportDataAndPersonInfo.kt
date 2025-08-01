package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportData

data class BookingsReportDataAndPersonInfo(
  val bookingsReportData: BookingsReportData,
  val personInfoReportData: PersonInformationReportData,
)
