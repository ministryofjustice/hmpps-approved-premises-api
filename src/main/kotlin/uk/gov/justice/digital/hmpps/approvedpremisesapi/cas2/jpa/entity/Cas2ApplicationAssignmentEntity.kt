package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2ApplicationAssignmentRepository : JpaRepository<Cas2ApplicationAssignmentEntity, UUID> {
  fun findFirstByApplicationIdOrderByCreatedAtDesc(applicationId: UUID): Cas2ApplicationAssignmentEntity?
}

@Entity
@Table(name = "cas_2_application_assignments")
data class Cas2ApplicationAssignmentEntity(
  @Id
  val id: UUID,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  val application: Cas2ApplicationEntity,
  val prisonCode: String,

  /*
   * BAIL-WIP - This should be migrated to CAS2 User when the system is stable but the prison assignment work did not
   * exist when the bail system was branched and it will only ever be a nomis user that is assigned a transfer.
   */
  @ManyToOne
  @JoinColumn(name = "allocated_pom_cas_2_user_id")
  val allocatedPomUser: Cas2UserEntity? = null,

  val createdAt: OffsetDateTime,
)
