package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "local_authority_areas")
data class LocalAuthorityAreaEntity(
  @Id
  var id: UUID,
  var identifier: String,
  var name: String,
  @OneToMany(mappedBy = "localAuthorityArea")
  var premises: MutableList<PremisesEntity>
)
