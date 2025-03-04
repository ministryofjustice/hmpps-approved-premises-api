package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2OffenderRepository : JpaRepository<Cas2OffenderEntity, UUID> {
  fun findByNomsNumber(nomsNumber: String): Cas2OffenderEntity?
}

@Entity
@Table(name = "cas_2_offenders")
data class Cas2OffenderEntity(
  @Id
  val id: UUID,
  val nomsNumber: String,
  val crn: String,
  val createdAt: OffsetDateTime,
  val updatedAt: OffsetDateTime,
)
