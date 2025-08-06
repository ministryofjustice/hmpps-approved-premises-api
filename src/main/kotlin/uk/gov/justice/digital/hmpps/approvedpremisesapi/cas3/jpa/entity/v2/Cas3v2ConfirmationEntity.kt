package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface Cas3v2ConfirmationRepository : JpaRepository<Cas3v2ConfirmationEntity, UUID>

@Entity
@Table(name = "cas3_confirmations")
data class Cas3v2ConfirmationEntity(
  @Id
  val id: UUID,
  val dateTime: OffsetDateTime,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  var booking: Cas3BookingEntity,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3v2ConfirmationEntity) return false

    if (id != other.id) return false
    if (dateTime != other.dateTime) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(dateTime, notes, createdAt)

  override fun toString() = "Cas3v2ConfirmationEntity:$id"
}
