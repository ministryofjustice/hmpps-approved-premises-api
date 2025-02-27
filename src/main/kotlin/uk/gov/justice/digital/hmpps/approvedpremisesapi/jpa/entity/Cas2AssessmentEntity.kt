package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLOrder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2AssessmentRepository : JpaRepository<Cas2AssessmentEntity, UUID> {
  fun findFirstByApplicationId(applicationId: UUID): Cas2AssessmentEntity?
}

@Entity
@Table(name = "cas_2_assessments")
data class Cas2AssessmentEntity(
  @Id
  val id: UUID,

  @OneToOne
  @JoinColumn(name = "application_id")
  val application: Cas2ApplicationEntity,

  val createdAt: OffsetDateTime,

  var nacroReferralId: String? = null,

  var assessorName: String? = null,

  @OneToMany(mappedBy = "assessment")
  @SQLOrder("createdAt DESC")
  var statusUpdates: MutableList<Cas2StatusUpdateEntity>? = null,
) {
  override fun toString() = "Cas2AssessmentEntity: $id"
}
