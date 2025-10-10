package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas3BedspaceCharacteristicRepository : JpaRepository<Cas3BedspaceCharacteristicEntity, UUID> {
  fun findByActive(active: Boolean): List<Cas3BedspaceCharacteristicEntity>

  fun findByName(name: String): Cas3BedspaceCharacteristicEntity?
}

@Entity
@Table(name = "cas3_bedspace_characteristics")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3BedspaceCharacteristicEntity(
  @Id
  val id: UUID,
  val name: String?,
  val description: String,
  @Column(name = "is_active")
  val active: Boolean,
)
