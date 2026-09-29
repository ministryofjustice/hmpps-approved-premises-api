package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding.statusDetailsByStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusDetail
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
  override fun toString() = "Cas2StatusDetailEntity: $id"

  fun statusDetail(statusId: UUID, detailId: UUID, service: ServiceName): Cas2AssessmentStatusDetail {
    val status = Cas2AssessmentStatus.entries.find { it.id == statusId } ?: throw IllegalStateException("Status not found for id $statusId")
    return statusDetailsByStatus(status, service)?.find { it.id == detailId } ?: throw IllegalStateException("Status detail not found for id $detailId")
  }
}
