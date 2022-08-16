package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface LostBedsRepository : JpaRepository<LostBedsEntity, UUID>

@Entity
@Table(name = "lost_beds")
data class LostBedsEntity(
  @Id
  val id: UUID,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val numberOfBeds: Int,
  @Enumerated(EnumType.STRING)
  val reason: LostBedReason,
  val referenceNumber: String?,
  val notes: String?,
  @OneToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LostBedsEntity) return false

    if (id != other.id) return false
    if (startDate != other.startDate) return false
    if (endDate != other.endDate) return false
    if (numberOfBeds != other.numberOfBeds) return false
    if (reason != other.reason) return false
    if (referenceNumber != other.referenceNumber) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, startDate, endDate, numberOfBeds, reason, referenceNumber, notes)

  override fun toString() = "ArrivalEntity:$id"
}

enum class LostBedReason() {
  Fire,
  Damaged,
  Refurbishment,
  StaffShortage
}
