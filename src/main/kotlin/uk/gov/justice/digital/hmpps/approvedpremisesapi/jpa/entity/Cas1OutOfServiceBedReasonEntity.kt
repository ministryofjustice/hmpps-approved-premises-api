package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1OutOfServiceBedReasonRepository : JpaRepository<Cas1OutOfServiceBedReasonEntity, UUID> {
  companion object {
    val BED_ON_HOLD_REASON_ID: UUID = UUID.fromString("199bef2d-0839-40c0-85e2-00b84fde7fde")
  }

  @Query("SELECT r FROM Cas1OutOfServiceBedReasonEntity r WHERE r.isActive = true")
  fun findActive(): List<Cas1OutOfServiceBedReasonEntity>
}

@Entity
@Table(name = "cas1_out_of_service_bed_reasons")
data class Cas1OutOfServiceBedReasonEntity(
  @Id
  val id: UUID,
  val createdAt: OffsetDateTime,
  val name: String,
  val isActive: Boolean,
)
