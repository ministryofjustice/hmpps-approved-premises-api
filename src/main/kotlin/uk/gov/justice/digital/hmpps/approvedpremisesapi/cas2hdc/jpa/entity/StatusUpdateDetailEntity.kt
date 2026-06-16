package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcPersistedApplicationStatusFinder
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

  var createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
  companion object {
    private val statusFinder = Cas2HdcPersistedApplicationStatusFinder()
  }

  override fun toString() = "Cas2StatusDetailEntity: $id"

  fun statusDetail(statusId: UUID, detailId: UUID): Cas2PersistedApplicationStatusDetail = statusFinder.getById(statusId).statusDetails
    ?.find { detail -> detail.id == detailId }
    ?: error("Status detail with id $detailId not found")
}
