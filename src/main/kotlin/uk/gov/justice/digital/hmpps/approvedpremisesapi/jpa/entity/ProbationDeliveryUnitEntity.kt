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
interface ProbationDeliveryUnitRepository : JpaRepository<ProbationDeliveryUnitEntity, UUID> {
  fun findAllByProbationRegion_Id(probationRegionId: UUID): List<ProbationDeliveryUnitEntity>

  fun findByIdAndProbationRegion_Id(id: UUID, probationRegionId: UUID): ProbationDeliveryUnitEntity?

  fun findByNameAndProbationRegion_Id(name: String, probationRegionId: UUID): ProbationDeliveryUnitEntity?

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
