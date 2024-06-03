package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

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
