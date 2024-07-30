package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface TurnaroundRepository : JpaRepository<TurnaroundEntity, UUID>

@Entity
@Table(name = "turnarounds")
data class TurnaroundEntity(
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
