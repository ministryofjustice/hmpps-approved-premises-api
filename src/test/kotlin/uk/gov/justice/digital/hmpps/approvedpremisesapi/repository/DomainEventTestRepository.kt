package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.util.UUID

@Repository
interface DomainEventTestRepository : JpaRepository<DomainEventEntity, UUID> {
  fun findFirstByOrderByCreatedAtDesc(): DomainEventEntity?
  fun findByApplicationId(applicationId: UUID): List<DomainEventEntity>
  fun findByAssessmentIdAndType(assessmentId: UUID, type: DomainEventType): List<DomainEventEntity>
  fun findByApplicationIdAndType(applicationId: UUID, type: DomainEventType): List<DomainEventEntity>
  fun findByCas3PremisesIdAndType(cas3PremisesId: UUID, type: DomainEventType): List<DomainEventEntity>
  fun findByCas3BedspaceId(cas3BedspaceId: UUID): List<DomainEventEntity>
  fun findByCas3TransactionId(cas3TransactionId: UUID): List<DomainEventEntity>

  @Query(
    """
      SELECT d 
      FROM  DomainEventEntity d 
      WHERE d.type in :domainEventTypes
      """,
  )
  fun findByTypes(domainEventTypes: List<DomainEventType>): List<DomainEventEntity>
}
