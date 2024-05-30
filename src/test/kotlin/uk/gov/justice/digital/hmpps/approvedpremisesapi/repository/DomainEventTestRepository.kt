package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import java.util.UUID

@Repository
interface DomainEventTestRepository : JpaRepository<DomainEventEntity, UUID> {
  fun findFirstByOrderByCreatedAtDesc(): DomainEventEntity?
  fun findByApplicationId(applicationId: UUID): List<DomainEventEntity>
}
