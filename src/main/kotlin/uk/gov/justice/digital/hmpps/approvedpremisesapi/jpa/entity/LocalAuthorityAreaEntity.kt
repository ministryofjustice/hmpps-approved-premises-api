package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
@Repository
interface LocalAuthorityAreaRepository : JpaRepository<LocalAuthorityAreaEntity, UUID> {
  fun findByName(name: String): LocalAuthorityAreaEntity?
}

@Entity
@Table(name = "local_authority_areas")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class LocalAuthorityAreaEntity(
  @Id
  var id: UUID,
  var identifier: String,
  var name: String,
  @OneToMany(mappedBy = "localAuthorityArea")
  var premises: MutableList<PremisesEntity>,
)
