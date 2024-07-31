package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Repository
interface AssessmentTestRepository : JpaRepository<ApprovedPremisesAssessmentEntity, UUID> {
  fun findAllByApplication(application: ApplicationEntity): List<ApprovedPremisesAssessmentEntity>

  @Transactional
  @Modifying
  @Query(
    "UPDATE assessments SET created_at = :createdAt WHERE application_id = :applicationId AND reallocated_at IS NULL",
    nativeQuery = true,
  )
  fun updateCreatedAtOnLatestAssessment(createdAt: OffsetDateTime, applicationId: UUID)
}
