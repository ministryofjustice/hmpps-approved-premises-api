package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.QueryHint
import javax.persistence.Table
import javax.persistence.Transient

@Repository
interface ProbationRegionRepository : JpaRepository<ProbationRegionEntity, UUID> {
  fun findByName(name: String): ProbationRegionEntity?
  fun findByDeliusCode(deliusCode: String): ProbationRegionEntity?

  @Query("SELECT DISTINCT p FROM ProbationRegionEntity p LEFT JOIN FETCH p.apArea WHERE p IN (:probationRegions)")
  @QueryHints(value = [QueryHint(name = org.hibernate.jpa.QueryHints.HINT_PASS_DISTINCT_THROUGH, value = "false")], forCounting = false)
  fun loadApAreas(probationRegions: List<ProbationRegionEntity>): List<ProbationRegionEntity>
}

@Entity
@Table(name = "probation_regions")
data class ProbationRegionEntity(
  @Id
  val id: UUID,
  val name: String,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ap_area_id")
  val apArea: ApAreaEntity,
  @OneToMany(mappedBy = "probationRegion")
  val premises: MutableList<PremisesEntity>,
  val deliusCode: String,
) {
  @Transient
  var apAreaLoaded = false
}
