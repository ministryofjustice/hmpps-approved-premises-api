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
  var id: UUID,
  var name: String,
  @OneToMany(mappedBy = "apArea")
  var premises: MutableList<PremisesEntity>
)
