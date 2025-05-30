package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Deprecated(
  "Bed moves were exclusively used by CAS1. There usage ended mid 2024." +
    "Once all CAS1 bookings have been migrated to space bookings this repo can be removed",
)
@Repository
interface BedMoveRepository : JpaRepository<BedMoveEntity, UUID> {
  fun deleteByBooking(booking: BookingEntity)
}

@Deprecated(
  "Bed moves were exclusively used by CAS1. There usage ended mid 2024." +
    "Once all CAS1 bookings have been migrated to space bookings this table can be removed",
)
@Entity
@Table(name = "bed_moves")
data class BedMoveEntity(
  @Id
  val id: UUID,

  @OneToOne
  @JoinColumn(name = "booking_id")
  val booking: BookingEntity,

  @OneToOne
  @JoinColumn(name = "previous_bed_id")
  val previousBed: BedEntity?,

  @OneToOne
  @JoinColumn(name = "new_bed_id")
  val newBed: BedEntity,

  var createdAt: OffsetDateTime,

  val notes: String?,
) {

  override fun toString() = "BedMovesEntity: $id"
}
