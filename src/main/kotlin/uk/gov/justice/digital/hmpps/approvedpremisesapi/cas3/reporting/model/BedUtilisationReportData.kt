package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBookingCancellationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationVoidBedspaceReportData

data class BedUtilisationReportData(
  val bedspaceReportData: BedUtilisationBedspaceReportData,
  val bookingsReportData: List<BedUtilisationBookingReportData>,
  val bookingCancellationReportData: List<BedUtilisationBookingCancellationReportData>,
  val bookingTurnaroundReportData: List<BedUtilisationBookingTurnaroundReportData>,
  val voidBedspaceReportData: List<BedUtilisationVoidBedspaceReportData>,
)
