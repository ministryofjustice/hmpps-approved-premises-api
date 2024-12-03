package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail

import jakarta.persistence.*
import org.springframework.context.annotation.Lazy

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2BailAssessmentRepository : JpaRepository<Cas2BailAssessmentEntity, UUID> {
  fun findFirstByApplicationId(applicationId: UUID): Cas2BailAssessmentEntity?
}

@Entity
@Table(name = "cas_2_bail_assessments")
data class Cas2BailAssessmentEntity(
  @Id
  val id: UUID,

  @OneToOne
  val application: Cas2BailApplicationEntity,

  val createdAt: OffsetDateTime,

  var nacroReferralId: String? = null,

  var assessorName: String? = null,

  @OneToMany(mappedBy = "assessment")
  @OrderBy("createdAt DESC")
  var statusUpdates: MutableList<Cas2BailStatusUpdateEntity>? = null,
) {
  override fun toString() = "Cas2BailAssessmentEntity: $id"
}