package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface DateChangeRepository : JpaRepository<DateChangeEntity, UUID>

@Entity
@Table(name = "date_changes")
data class DateChangeEntity(
  @Id
  val id: UUID,
  val changedAt: OffsetDateTime,
  val previousArrivalDate: LocalDate,
  val previousDepartureDate: LocalDate,
  val newArrivalDate: LocalDate,
  val newDepartureDate: LocalDate,
  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity,
  @ManyToOne
  @JoinColumn(name = "changed_by_user_id")
  var changedByUser: UserEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DateChangeEntity

    if (id != other.id) return false
    if (changedAt != other.changedAt) return false
    if (previousArrivalDate != other.previousArrivalDate) return false
    if (previousDepartureDate != other.previousDepartureDate) return false
    if (newArrivalDate != other.newArrivalDate) return false
    return newDepartureDate == other.newDepartureDate
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + changedAt.hashCode()
    result = 31 * result + previousArrivalDate.hashCode()
    result = 31 * result + previousDepartureDate.hashCode()
    result = 31 * result + newArrivalDate.hashCode()
    result = 31 * result + newDepartureDate.hashCode()
    return result
  }

  override fun toString() = "DateChangeEntity:$id"
}
