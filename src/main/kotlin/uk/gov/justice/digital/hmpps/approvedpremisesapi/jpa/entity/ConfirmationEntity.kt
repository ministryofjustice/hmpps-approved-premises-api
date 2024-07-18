package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface ConfirmationRepository : JpaRepository<ConfirmationEntity, UUID>

@Entity
@Table(name = "confirmations")
data class ConfirmationEntity(
  @Id
  val id: UUID,
  val dateTime: OffsetDateTime,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @OneToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ConfirmationEntity) return false

    if (id != other.id) return false
    if (dateTime != other.dateTime) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(dateTime, notes, createdAt)

  override fun toString() = "ConfirmationEntity:$id"
}
