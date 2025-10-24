package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import java.util.UUID

interface BedUsageRepository : JpaRepository<BedEntity, UUID> {

  @Query(
    """
    SELECT b.*
    FROM beds b 
    INNER JOIN rooms r ON b.room_id = r.id
    INNER JOIN premises p ON r.premises_id = p.id
    WHERE p.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR p.probation_region_id = :probationRegionId)
    ORDER BY r.name
    """,
    nativeQuery = true,
  )
  fun findAllBedspaces(
    probationRegionId: UUID?,
  ): List<BedEntity>

  @Query(
    """
    SELECT b.*
    FROM cas3_bedspaces b 
    INNER JOIN cas3_premises p ON b.premises_id = p.id
    LEFT JOIN probation_delivery_units pdu ON p.probation_delivery_unit_id = pdu.id
    WHERE (CAST(:probationRegionId AS UUID) IS NULL OR pdu.probation_region_id = :probationRegionId)
    ORDER BY b.reference
    """,
    nativeQuery = true,
  )
  fun findAllBedspacesV2(
    probationRegionId: UUID?,
  ): List<Cas3BedspacesEntity>
}
