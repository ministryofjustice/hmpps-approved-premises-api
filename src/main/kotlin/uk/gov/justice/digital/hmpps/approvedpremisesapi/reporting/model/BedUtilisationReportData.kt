package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBookingCancellationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationVoidBedspaceReportData

data class BedUtilisationReportData(
  val bedspaceReportData: BedUtilisationBedspaceReportData,
  val bookingsReportData: List<BedUtilisationBookingReportData>,
  val bookingCancellationReportData: List<BedUtilisationBookingCancellationReportData>,
  val bookingTurnaroundReportData: List<BedUtilisationBookingTurnaroundReportData>,
  val voidBedspaceReportData: List<BedUtilisationVoidBedspaceReportData>,
)
