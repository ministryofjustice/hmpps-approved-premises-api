package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesCharacteristic
import java.util.UUID

@Repository
interface Cas3PremisesCharacteristicRepository : JpaRepository<Cas3PremisesCharacteristicEntity, UUID> {
  @Query("select c from Cas3PremisesCharacteristicEntity c where c.id in (:ids) and c.isActive = true")
  fun findActiveCharacteristicsByIdIn(ids: List<UUID>): List<Cas3PremisesCharacteristicEntity>
}

@Entity
@Table(name = "cas3_premises_characteristics")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3PremisesCharacteristicEntity(
  @Id
  var id: UUID,
  var name: String,
  var description: String,
  var isActive: Boolean,
) {
  fun toCas3PremisesCharacteristic() = Cas3PremisesCharacteristic(
    id = this.id,
    name = this.name,
    description = this.description,
  )
}
