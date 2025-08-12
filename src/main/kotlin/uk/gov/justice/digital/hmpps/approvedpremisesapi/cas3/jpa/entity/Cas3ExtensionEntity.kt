package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface Cas3ExtensionRepository : JpaRepository<Cas3ExtensionEntity, UUID>

@Entity
@Table(name = "extensions")
data class Cas3ExtensionEntity(
  @Id
  val id: UUID,
  val previousDepartureDate: LocalDate,
  val newDepartureDate: LocalDate,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  var booking: Cas3BookingEntity,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3ExtensionEntity) return false

    if (id != other.id) return false
    if (previousDepartureDate != other.previousDepartureDate) return false
    if (newDepartureDate != other.newDepartureDate) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, previousDepartureDate, newDepartureDate, notes, createdAt)

  override fun toString() = "Cas3ExtensionEntity:$id"
}
