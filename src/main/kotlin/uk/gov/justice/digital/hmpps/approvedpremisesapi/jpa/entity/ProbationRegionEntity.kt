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
interface ProbationRegionRepository : JpaRepository<ProbationRegionEntity, UUID> {
  fun findByName(name: String): ProbationRegionEntity?
}

@Entity
@Table(name = "probation_regions")
data class ProbationRegionEntity(
  @Id
  val id: UUID,
  val name: String,
  /**
   * If the AP Area for a user is required, instead use [UserEntity.apArea]
   *
   * This will only ever be null for the 'National' region, which will
   * never be associated with a corresponding [PremisesEntity]. This
   * model inconsistency could be removed by using separate models
   * for premises regions and user regions
   */
  @ManyToOne
  @JoinColumn(name = "ap_area_id")
  val apArea: ApAreaEntity?,
  @Deprecated(message = "There can be multiple codes to a region, so use probation_area_probation_region_mappings instead")
  val deliusCode: String,
) {
  override fun toString() = "ProbationRegionEntity:$id"
}
