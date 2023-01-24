package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface OfflineApplicationRepository : JpaRepository<OfflineApplicationEntity, UUID> {
  fun findAllWhereService(name: String): List<OfflineApplicationEntity>
}

@Entity
@Table(name = "offline_applications")
data class OfflineApplicationEntity(
  @Id
  val id: UUID,
  val crn: String,
  val service: String,
  val submittedAt: OffsetDateTime,
  val createdAt: OffsetDateTime
)
