package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas3LostBedReasonRepository : JpaRepository<Cas3LostBedReasonEntity, UUID> {
  @Query("SELECT l FROM Cas3LostBedReasonEntity l WHERE l.serviceScope = :serviceName OR l.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<Cas3LostBedReasonEntity>
}

@Entity
@Table(name = "lost_bed_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3LostBedReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
) {
  override fun toString() = "Cas3LostBedReasonEntity:$id"
}
