package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

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
interface Cas3VoidBedspaceCancellationRepository : JpaRepository<Cas3VoidBedspaceCancellationEntity, UUID>

@Entity
@Table(name = "cas3_void_bedspace_cancellations")
data class Cas3VoidBedspaceCancellationEntity(
  @Id
  val id: UUID,
  val createdAt: OffsetDateTime,
  val notes: String?,
  @OneToOne
  @JoinColumn(name = "cas3_void_bedspace_id")
  val voidBedspace: Cas3VoidBedspaceEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3VoidBedspaceCancellationEntity) return false

    if (id != other.id) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(notes, createdAt)

  override fun toString() = "Cas3VoidBedspaceCancellationEntity:$id"
}
