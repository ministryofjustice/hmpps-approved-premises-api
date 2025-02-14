package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2StatusUpdateDetailRepository : JpaRepository<Cas2StatusUpdateDetailEntity, UUID> {
  fun findFirstByStatusUpdateIdOrderByCreatedAtDesc(statusUpdateId: UUID): Cas2StatusUpdateDetailEntity?
}

@Entity
@Table(name = "cas_2_status_update_details")
data class Cas2StatusUpdateDetailEntity(
  @Id
  val id: UUID,
  val statusDetailId: UUID,

  val label: String,

  @ManyToOne
  @JoinColumn(name = "status_update_id")
  val statusUpdate: Cas2StatusUpdateEntity,

  @CreationTimestamp
  var createdAt: OffsetDateTime? = null,
) {
  override fun toString() = "Cas2StatusDetailEntity: $id"

  fun statusDetail(statusId: UUID, detailId: UUID): Cas2PersistedApplicationStatusDetail = Cas2PersistedApplicationStatusFinder().getById(statusId).statusDetails?.find { detail -> detail.id == detailId }
    ?: error("Status detail with id $detailId not found")
}
