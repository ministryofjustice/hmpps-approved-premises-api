package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface LostBedReasonRepository : JpaRepository<LostBedReasonEntity, UUID>

@Entity
@Table(name = "lost_bed_reasons")
data class LostBedReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean
) {
  override fun toString() = "LostBedReasonEntity:$id"
}
