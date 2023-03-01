package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface DepartureReasonRepository : JpaRepository<DepartureReasonEntity, UUID> {
  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.serviceScope = :serviceName OR d.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<DepartureReasonEntity>
}

@Entity
@Table(name = "departure_reasons")
data class DepartureReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val legacyDeliusReasonCode: String?
) {
  override fun toString() = "DepartureReasonEntity:$id"
}
