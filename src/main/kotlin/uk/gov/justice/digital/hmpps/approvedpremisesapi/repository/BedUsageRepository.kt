package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
    """,
    nativeQuery = true,
  )
  fun findAllBedspaces(
    probationRegionId: UUID?,
  ): List<BedEntity>
}
