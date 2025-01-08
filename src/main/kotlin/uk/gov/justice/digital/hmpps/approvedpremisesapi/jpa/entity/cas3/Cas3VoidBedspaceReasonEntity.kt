package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas3VoidBedspaceReasonRepository : JpaRepository<Cas3VoidBedspaceReasonEntity, UUID>

@Entity
@Table(name = "cas3_void_bedspace_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3VoidBedspaceReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
) {
  override fun toString() = "Cas3VoidBedspaceReasonEntity:$id"
}
