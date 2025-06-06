package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas3BedspaceCharacteristicRepository : JpaRepository<Cas3BedspaceCharacteristicEntity, UUID>

@Entity
@Table(name = "cas3_bedspace_characteristics")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3BedspaceCharacteristicEntity(
  @Id
  var id: UUID,
  var code: String?,
  var name: String,
  var isActive: Boolean,
)
