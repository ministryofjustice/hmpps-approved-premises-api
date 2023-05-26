package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "ap_areas")
data class ApAreaEntity(
  @Id
  val id: UUID,
  val name: String,
  val identifier: String,
  @OneToMany(mappedBy = "apArea")
  val probationRegions: MutableList<ProbationRegionEntity>,
)
