package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Column
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

  companion object Constants {
    const val CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY = "isSingleOccupancy"
    const val CAS3_PROPERTY_NAME_SHARED_PROPERTY = "isSharedProperty"
    const val CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE = "isWheelchairAccessible"
    const val CAS3_PROPERTY_NAME_MEN_ONLY = "isMenOnly"
    const val CAS3_PROPERTY_NAME_WOMEN_ONLY = "isWomenOnly"
    const val CAS3_PROPERTY_NAME_PUB_NEAR_BY = "isPubNearBy"
  }

  fun findByActive(active: Boolean): List<Cas3PremisesCharacteristicEntity>

  @Query("select c from Cas3PremisesCharacteristicEntity c where c.id in (:ids) and c.active = true")
  fun findActiveCharacteristicsByIdIn(ids: List<UUID>): List<Cas3PremisesCharacteristicEntity>

  fun findByName(name: String): Cas3PremisesCharacteristicEntity?
}

@Entity
@Table(name = "cas3_premises_characteristics")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3PremisesCharacteristicEntity(
  @Id
  val id: UUID,
  val name: String,
  val description: String,
  @Column(name = "is_active")
  val active: Boolean,
) {
  fun toCas3PremisesCharacteristic() = Cas3PremisesCharacteristic(
    id = this.id,
    name = this.name,
    description = this.description,
  )
}
