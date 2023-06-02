package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface CancellationReasonRepository : JpaRepository<CancellationReasonEntity, UUID> {
  @Query("SELECT c FROM CancellationReasonEntity c WHERE c.serviceScope = :serviceName OR c.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<CancellationReasonEntity>

  fun findByNameAndServiceScope(name: String, serviceScope: String): CancellationReasonEntity?
}

@Entity
@Table(name = "cancellation_reasons")
data class CancellationReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
) {
  override fun toString() = "CancellationReasonEntity:$id"
}
