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
interface NonArrivalReasonRepository : JpaRepository<NonArrivalReasonEntity, UUID> {

  companion object {
    val NON_ARRIVAL_REASON_CUSTODIAL_DISPOSAL_RIC: UUID = UUID.fromString("9d3b1f8e-9fa6-45b7-84ac-2d5fe34ff935")
  }

  @Query("SELECT r FROM NonArrivalReasonEntity r WHERE r.isActive = true")
  fun findAllActiveReasons(): List<NonArrivalReasonEntity>
}

@Entity
@Table(name = "non_arrival_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
data class NonArrivalReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val legacyDeliusReasonCode: String?,
) {
  override fun toString() = "NonArrivalReasonEntity:$id"
}
