package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Entity
@Table(name = "non_arrivals")
data class Cas3NonArrivalEntity(
  @Id
  val id: UUID,
  val date: LocalDate,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "non_arrival_reason_id")
  val reason: NonArrivalReasonEntity,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  var booking: Cas3BookingEntity,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3NonArrivalEntity) return false

    if (id != other.id) return false
    if (date != other.date) return false
    if (reason != other.reason) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, date, reason, notes, createdAt)

  override fun toString() = "Cas3NonArrivalEntity:$id"
}
