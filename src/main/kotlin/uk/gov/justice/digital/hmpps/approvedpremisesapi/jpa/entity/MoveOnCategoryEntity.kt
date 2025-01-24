package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MoveOnCategoryRepository : JpaRepository<MoveOnCategoryEntity, UUID> {
  companion object Constants {
    val NOT_APPLICABLE_MOVE_ON_CATEGORY_ID: UUID = UUID.fromString("ea3d79b0-1ee5-47ff-a7ae-b0d964ca7626")
  }

  @Query("SELECT d FROM MoveOnCategoryEntity d WHERE d.serviceScope = 'approved-premises' AND d.isActive = true")
  fun findActiveForCas1(): List<MoveOnCategoryEntity>

  @Query("SELECT m FROM MoveOnCategoryEntity m WHERE m.serviceScope IN (:serviceName, '*')")
  fun findAllByServiceScope(serviceName: String): List<MoveOnCategoryEntity>

  @Query("SELECT m FROM MoveOnCategoryEntity m WHERE m.isActive = true AND m.serviceScope IN (:serviceName, '*')")
  fun findActiveByServiceScope(serviceName: String): List<MoveOnCategoryEntity>

  @Query("SELECT m FROM MoveOnCategoryEntity m WHERE m.isActive = true")
  fun findActive(): List<MoveOnCategoryEntity>
}

@Entity
@Table(name = "move_on_categories")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class MoveOnCategoryEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  /**
   * Use of wildcard should be considered deprecated as blocks the ability to disable a reason
   * for one service but no the other. Wildcard is no longer used by CAS1
   */
  val serviceScope: String,
  val legacyDeliusCategoryCode: String?,
) {
  fun isCas1() = serviceScope == "approved-premises"

  override fun toString() = "MoveOnCategoryEntity:$id"
}

@Deprecated(
  """
  Use of wildcard should be considered deprecated as blocks the ability to disable a reason
  for one service but no the other. Wildcard is no longer used by CAS1
""",
)
fun MoveOnCategoryEntity.serviceScopeMatches(bookingService: String): Boolean {
  return when (serviceScope) {
    "*" -> true
    bookingService -> true
    else -> return false
  }
}
