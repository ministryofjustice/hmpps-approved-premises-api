package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface Cas3CancellationRepository : JpaRepository<Cas3CancellationEntity, UUID>

@Entity
@Table(name = "cancellations")
data class Cas3CancellationEntity(
  @Id
  val id: UUID,
  val date: LocalDate,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cancellation_reason_id")
  val reason: CancellationReasonEntity,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  var booking: Cas3BookingEntity,
  val otherReason: String?,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3CancellationEntity) return false

    if (id != other.id) return false
    if (date != other.date) return false
    if (reason != other.reason) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(date, reason, notes, createdAt)

  override fun toString() = "Cas3CancellationEntity:$id"
}
