package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceVoid
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingRecord
import java.time.LocalDate
import java.util.UUID

@Repository
interface Cas3BookingGapReportRepository : JpaRepository<Cas3VoidBedspaceEntity, UUID> {

  @Query(
    """
       SELECT bedspace.id,
           premises.name AS premises_name,
           bedspace.reference AS room_name,
           probation_regions.name AS probation_region,
           pdu.name AS pdu_name,
           bedspace.startDate,
           bedspace.endDate
        FROM Cas3BedspacesEntity bedspace
        INNER JOIN Cas3PremisesEntity premises ON bedspace.premises.id = premises.id
        INNER JOIN ProbationDeliveryUnitEntity pdu ON pdu.id = premises.probationDeliveryUnit.id
        INNER JOIN ProbationRegionEntity probation_regions ON probation_regions.id = pdu.probationRegion.id
        WHERE (bedspace.endDate IS NULL OR bedspace.endDate >= :startDate) AND bedspace.startDate <= :endDate
    """,
  )
  fun getBedspacesV2(startDate: LocalDate, endDate: LocalDate): List<BedspaceInfo>

  @Query(name = "Cas3BookingEntity.getBookingsV2", nativeQuery = true)
  fun getBookingsV2(startDate: LocalDate, endDate: LocalDate): List<BookingRecord>

  @Query(
    """
        SELECT void.bedspace.id AS bed_id,
            void.startDate,
            void.endDate
        FROM Cas3VoidBedspaceEntity void
        WHERE void.cancellationDate IS NULL AND void.endDate >= :startDate
      """,
  )
  fun getBedspaceVoidsV2(startDate: LocalDate): List<BedspaceVoid>
}
