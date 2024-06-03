package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface OfflineApplicationRepository : JpaRepository<OfflineApplicationEntity, UUID> {
  fun findAllByService(name: String): List<OfflineApplicationEntity>
  fun findAllByServiceAndCrn(name: String, crn: String): List<OfflineApplicationEntity>
}

@Entity
@Table(name = "offline_applications")
data class OfflineApplicationEntity(
  @Id
  val id: UUID,
  val crn: String,
  val service: String,
  val createdAt: OffsetDateTime,
  val eventNumber: String?,
)
