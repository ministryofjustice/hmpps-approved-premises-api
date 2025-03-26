package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OffenderManagementUnitRepository : JpaRepository<OffenderManagementUnitEntity, UUID> {

  fun findByPrisonCode(prisonCode: String): OffenderManagementUnitEntity?
}

@Entity
@Table(name = "offender_management_units")
data class OffenderManagementUnitEntity(
  @Id
  val id: UUID,
  val prisonCode: String,
  val email: String,
)
