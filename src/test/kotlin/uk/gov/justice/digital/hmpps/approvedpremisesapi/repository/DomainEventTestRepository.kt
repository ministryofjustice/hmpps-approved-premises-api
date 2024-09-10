package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.util.UUID

@Repository
interface DomainEventTestRepository : JpaRepository<DomainEventEntity, UUID> {
  fun findFirstByOrderByCreatedAtDesc(): DomainEventEntity?
  fun findByApplicationId(applicationId: UUID): List<DomainEventEntity>
  fun findByAssessmentIdAndType(assessmentId: UUID, type: DomainEventType): List<DomainEventEntity>
}
