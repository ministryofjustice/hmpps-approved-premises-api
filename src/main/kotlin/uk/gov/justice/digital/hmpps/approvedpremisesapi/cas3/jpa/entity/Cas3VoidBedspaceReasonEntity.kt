package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.ReferenceData
import java.util.UUID

@Repository
interface Cas3VoidBedspaceReasonRepository : JpaRepository<Cas3VoidBedspaceReasonEntity, UUID> {

  @Query("SELECT v FROM Cas3VoidBedspaceReasonEntity v WHERE v.isActive = true ORDER by v.name ASC")
  fun findAllActive(): List<Cas3VoidBedspaceReasonEntity>
}

@Entity
@Table(name = "cas3_void_bedspace_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
data class Cas3VoidBedspaceReasonEntity(
  @Id
  override val id: UUID,
  override val name: String,
  @Transient // @Transient because this field is not in the database.
  override val description: String?,
  val isActive: Boolean,
) : ReferenceData {
  override fun toString() = "Cas3VoidBedspaceReasonEntity:$id"
}
