package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "cas3_turnarounds")
data class Cas3v2TurnaroundEntity(
  @Id
  val id: UUID,
  val workingDayCount: Int,
  val createdAt: OffsetDateTime,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  val booking: Cas3BookingEntity,
) {
  override fun toString() = "Cas3v2TurnaroundEntity: $id"
}

@Repository
interface Cas3v2TurnaroundRepository : JpaRepository<Cas3v2TurnaroundEntity, UUID>
