package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DepartureReasonRepository : JpaRepository<DepartureReasonEntity, UUID> {
  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.serviceScope = :serviceName OR d.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<DepartureReasonEntity>

  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.serviceScope = :serviceName OR d.serviceScope = '*' AND d.isActive = true")
  fun findActiveByServiceScope(serviceName: String): List<DepartureReasonEntity>

  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.isActive = true")
  fun findActive(): List<DepartureReasonEntity>

  fun findByNameAndServiceScope(name: String, serviceScope: String): DepartureReasonEntity?
}

@Entity
@Table(name = "departure_reasons")
data class DepartureReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val legacyDeliusReasonCode: String?,
) {
  override fun toString() = "DepartureReasonEntity:$id"
}
