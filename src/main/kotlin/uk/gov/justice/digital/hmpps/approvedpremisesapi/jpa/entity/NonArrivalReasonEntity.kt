package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface NonArrivalReasonRepository : JpaRepository<NonArrivalReasonEntity, UUID> {
  fun findByName(name: String): NonArrivalReasonEntity?
}

@Entity
@Table(name = "non_arrival_reasons")
data class NonArrivalReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val legacyDeliusReasonCode: String?,
) {
  override fun toString() = "NonArrivalReasonEntity:$id"
}
