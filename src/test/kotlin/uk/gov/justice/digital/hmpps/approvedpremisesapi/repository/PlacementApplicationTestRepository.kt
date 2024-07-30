package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Repository
interface PlacementApplicationTestRepository : JpaRepository<PlacementApplicationEntity, UUID> {
  fun findByApplicationAndReallocatedAtNull(application: ApplicationEntity): PlacementApplicationEntity

  @Transactional
  @Modifying
  @Query(
    "UPDATE placement_applications SET submitted_at = :submittedAt WHERE id = :id",
    nativeQuery = true,
  )
  fun updateSubmittedOn(id: UUID, submittedAt: OffsetDateTime)
}
