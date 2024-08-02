package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MoveOnCategoryRepository : JpaRepository<MoveOnCategoryEntity, UUID> {
  @Query("SELECT m FROM MoveOnCategoryEntity m WHERE m.serviceScope IN (:serviceName, '*')")
  fun findAllByServiceScope(serviceName: String): List<MoveOnCategoryEntity>

  @Query("SELECT m FROM MoveOnCategoryEntity m WHERE m.isActive = true AND m.serviceScope IN (:serviceName, '*')")
  fun findActiveByServiceScope(serviceName: String): List<MoveOnCategoryEntity>

  @Query("SELECT m FROM MoveOnCategoryEntity m WHERE m.isActive = true")
  fun findActive(): List<MoveOnCategoryEntity>

  fun findByNameAndServiceScope(name: String, serviceScope: String): MoveOnCategoryEntity?
}

@Entity
@Table(name = "move_on_categories")
data class MoveOnCategoryEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val legacyDeliusCategoryCode: String?,
) {
  override fun toString() = "MoveOnCategoryEntity:$id"
}
