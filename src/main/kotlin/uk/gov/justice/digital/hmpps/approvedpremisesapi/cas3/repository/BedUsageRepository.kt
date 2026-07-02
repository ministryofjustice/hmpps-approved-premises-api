package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import java.util.UUID

interface BedUsageRepository : JpaRepository<Cas3BedspacesEntity, UUID> {

  @Query(
    """
    SELECT b
    FROM Cas3BedspacesEntity b 
    JOIN FETCH b.premises p
    JOIN FETCH p.probationDeliveryUnit pdu
    JOIN FETCH pdu.probationRegion pr
    LEFT JOIN FETCH p.localAuthorityArea la
    WHERE (:probationRegionId IS NULL OR pr.id = :probationRegionId)
    ORDER BY b.reference
    """,
  )
  fun findAllBedspacesV2(
    probationRegionId: UUID?,
  ): List<Cas3BedspacesEntity>
}
