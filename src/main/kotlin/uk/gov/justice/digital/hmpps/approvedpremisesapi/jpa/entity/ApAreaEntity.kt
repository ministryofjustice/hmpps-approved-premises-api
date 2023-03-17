package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "ap_areas")
data class ApAreaEntity(
  @Id
  val id: UUID,
  val name: String,
  val identifier: String,
  @OneToMany(mappedBy = "apArea")
  val probationRegions: MutableList<ProbationRegionEntity>
)
