package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface DepartureRepository : JpaRepository<DepartureEntity, UUID>

@Entity
@Table(name = "departures")
data class DepartureEntity(
  @Id
  val id: UUID,
  val dateTime: OffsetDateTime,
  @ManyToOne
  @JoinColumn(name = "departure_reason_id")
  val reason: DepartureReasonEntity,
  @ManyToOne
  @JoinColumn(name = "move_on_category_id")
  val moveOnCategory: MoveOnCategoryEntity,
  @ManyToOne
  @JoinColumn(name = "destination_provider_id")
  val destinationProvider: DestinationProviderEntity?,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DepartureEntity) return false

    if (id != other.id) return false
    if (dateTime != other.dateTime) return false
    if (reason != other.reason) return false
    if (moveOnCategory != other.moveOnCategory) return false
    if (destinationProvider != other.destinationProvider) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(dateTime, reason, moveOnCategory, destinationProvider, notes, createdAt)

  override fun toString() = "DepartureEntity:$id"
}
