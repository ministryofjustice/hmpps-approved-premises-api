package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface NonArrivalRepository : JpaRepository<NonArrivalEntity, UUID>

@Entity
@Table(name = "non_arrivals")
data class NonArrivalEntity(
  @Id
  val id: UUID,
  val date: LocalDate,
  @ManyToOne
  @JoinColumn(name = "non_arrival_reason_id")
  val reason: NonArrivalReasonEntity,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @OneToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NonArrivalEntity) return false

    if (id != other.id) return false
    if (date != other.date) return false
    if (reason != other.reason) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, date, reason, notes, createdAt)

  override fun toString() = "NonArrivalEntity:$id"
}
