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
interface ProbationDeliveryUnitRepository : JpaRepository<ProbationDeliveryUnitEntity, UUID> {
  fun findAllByProbationRegionId(probationRegionId: UUID): List<ProbationDeliveryUnitEntity>

  fun findByIdAndProbationRegionId(id: UUID, probationRegionId: UUID): ProbationDeliveryUnitEntity?

  fun findByNameAndProbationRegionId(name: String, probationRegionId: UUID): ProbationDeliveryUnitEntity?

  fun findByDeliusCode(deliusCode: String): ProbationDeliveryUnitEntity?
}

@Entity
@Table(name = "probation_delivery_units")
data class ProbationDeliveryUnitEntity(
  @Id
  val id: UUID,
  val name: String,
  val deliusCode: String?,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  val probationRegion: ProbationRegionEntity,
)
