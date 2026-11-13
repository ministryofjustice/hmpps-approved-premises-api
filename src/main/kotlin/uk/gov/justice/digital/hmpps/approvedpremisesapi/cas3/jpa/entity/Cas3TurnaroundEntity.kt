package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas3TurnaroundRepository : JpaRepository<Cas3TurnaroundEntity, UUID>

/**
 * The turnaround period is the number of working days needed to prepare the room for the next stay (e.g. clean). A premises
 * has a default turnaround value ([Cas3PremisesEntity.turnaroundWorkingDays]), but this can be overridden for a booking
 *
 * Adding the turnaround working days to a booking end date determines the bedspace's last unavailable date, skipping
 * non-working days. For example, with no bank holidays, a Friday departure and two working days of turnaround
 * make Tuesday the last unavailable date; the bedspace becomes available on Wednesday.
 * 
 * Turnarounds can also be used as a mechanism to block access to a bedspace for a long period of time if it's going to
 * be 'out of service', e.g. decorations, repairs, temporary closure etc.
 */
@Entity
@Table(name = "cas3_turnarounds")
data class Cas3TurnaroundEntity(
  @Id
  val id: UUID,
  /**
   * >= 0
   */
  val workingDayCount: Int,
  val createdAt: OffsetDateTime,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  val booking: Cas3BookingEntity,
) {
  override fun toString() = "Cas3TurnaroundEntity: $id"
}
