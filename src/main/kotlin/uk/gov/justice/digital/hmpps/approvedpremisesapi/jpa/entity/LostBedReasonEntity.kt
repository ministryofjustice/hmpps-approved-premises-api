package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface LostBedReasonRepository : JpaRepository<LostBedReasonEntity, UUID> {
  @Query("SELECT l FROM LostBedReasonEntity l WHERE l.serviceScope = :serviceName OR l.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<LostBedReasonEntity>
}

@Entity
@Table(name = "lost_bed_reasons")
data class LostBedReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
) {
  override fun toString() = "LostBedReasonEntity:$id"
}
