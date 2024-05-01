package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface ReferralRejectionReasonRepository : JpaRepository<ReferralRejectionReasonEntity, UUID> {
  @Query("SELECT m FROM ReferralRejectionReasonEntity m WHERE m.serviceScope = :serviceName OR m.serviceScope = '*' ORDER BY m.sortOrder")
  fun findAllByServiceScope(serviceName: String): List<ReferralRejectionReasonEntity>

  @Query("SELECT m FROM ReferralRejectionReasonEntity m WHERE m.serviceScope = :serviceName OR m.serviceScope = '*' AND m.isActive = true ORDER BY m.sortOrder")
  fun findActiveByServiceScope(serviceName: String): List<ReferralRejectionReasonEntity>

  @Query("SELECT m FROM ReferralRejectionReasonEntity m WHERE m.isActive = true ORDER BY m.sortOrder")
  fun findActive(): List<ReferralRejectionReasonEntity>
}

@Entity
@Table(name = "referral_rejection_reasons")
data class ReferralRejectionReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val sortOrder: Int,
) {
  override fun toString() = "ReferralRejectionReasonEntity:$id"
}
