package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface ExtensionRepository : JpaRepository<ExtensionEntity, UUID>

@Entity
@Table(name = "extensions")
data class ExtensionEntity(
  @Id
  val id: UUID,
  val previousDepartureDate: LocalDate,
  val newDepartureDate: LocalDate,
  val notes: String?,
  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ExtensionEntity) return false

    if (id != other.id) return false
    if (previousDepartureDate != other.previousDepartureDate) return false
    if (newDepartureDate != other.newDepartureDate) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, previousDepartureDate, newDepartureDate, notes)

  override fun toString() = "ExtensionEntity:$id"
}
