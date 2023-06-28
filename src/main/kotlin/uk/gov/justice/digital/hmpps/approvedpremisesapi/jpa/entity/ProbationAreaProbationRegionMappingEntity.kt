package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface ProbationAreaProbationRegionMappingRepository : JpaRepository<ProbationAreaProbationRegionMappingEntity, UUID> {
  fun findByProbationAreaDeliusCode(probationAreaDeliusCode: String): ProbationAreaProbationRegionMappingEntity?
}

@Entity
@Table(name = "probation_area_probation_region_mappings")
data class ProbationAreaProbationRegionMappingEntity(
  @Id
  val id: UUID,
  val probationAreaDeliusCode: String,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  val probationRegion: ProbationRegionEntity,
)
