package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas3TurnaroundRepository : JpaRepository<Cas3TurnaroundEntity, UUID>

@Entity
@Table(name = "cas3_turnarounds")
data class Cas3TurnaroundEntity(
  @Id
  val id: UUID,
  val workingDayCount: Int,
  val createdAt: OffsetDateTime,
  @ManyToOne
  @JoinColumn(name = "booking_id")
  val booking: BookingEntity,
) {
  override fun toString() = "TurnaroundEntity: $id"
}
