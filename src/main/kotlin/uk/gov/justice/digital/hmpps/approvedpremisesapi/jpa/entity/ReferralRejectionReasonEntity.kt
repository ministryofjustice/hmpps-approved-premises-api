package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReferralRejectionReasonRepository : JpaRepository<ReferralRejectionReasonEntity, UUID> {
  @Query("SELECT rr FROM ReferralRejectionReasonEntity rr WHERE rr.name = :name AND rr.isActive = true")
  fun findByNameAndActive(name: String): ReferralRejectionReasonEntity?

  @Query("SELECT rr FROM ReferralRejectionReasonEntity rr WHERE rr.isActive = true ORDER BY rr.name")
  fun findAllActive(): List<ReferralRejectionReasonEntity>
}

@Entity
@Table(name = "referral_rejection_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
data class ReferralRejectionReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
) {
  override fun toString() = "ReferralRejectionReasonEntity:$id"
}
