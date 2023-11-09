package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult

data class BookingsReportData(
  val booking: BookingEntity,
  val personInfoResult: PersonSummaryInfoResult,
)
