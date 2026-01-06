package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity

data class BedspaceUsageReportData(
  val bedspace: Cas3BedspacesEntity,
  val bookings: List<Cas3BookingEntity>,
  val voids: List<Cas3VoidBedspaceEntity>,
)
