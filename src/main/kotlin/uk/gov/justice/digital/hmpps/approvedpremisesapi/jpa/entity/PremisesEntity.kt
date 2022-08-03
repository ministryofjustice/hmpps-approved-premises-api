package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "premises")
data class PremisesEntity(
  @Id
  var id: UUID,
  var name: String,
  var apCode: String,
  var postcode: String,
  var totalBeds: Int,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  var probationRegion: ProbationRegionEntity,
  @ManyToOne
  @JoinColumn(name = "ap_area_id")
  var apArea: ApAreaEntity,
  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity
)
