package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas1CruManagementAreaRepository : JpaRepository<Cas1CruManagementAreaEntity, UUID>

/**
 * When an application is submitted it will be assigned to
 * a CRU Management Area. This will then be used to determine
 * which CRU Members should manage assessment and matching
 * for the application
 */
@Entity
@Table(name = "cas1_cru_management_areas")
data class Cas1CruManagementAreaEntity(
  @Id
  val id: UUID,
  var name: String,
  var emailAddress: String?,
  val notifyReplyToEmailId: String?,
  var assessmentAutoAllocationUsername: String?,
) {
  companion object {
    val WOMENS_ESTATE_ID = UUID.fromString("bfb04c2a-1954-4512-803d-164f7fcf252c")
  }
}
