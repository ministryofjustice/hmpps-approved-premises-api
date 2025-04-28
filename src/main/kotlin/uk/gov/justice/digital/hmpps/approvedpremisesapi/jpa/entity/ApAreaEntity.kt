package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ApAreaRepository : JpaRepository<ApAreaEntity, UUID> {
  fun findByIdentifier(name: String): ApAreaEntity?
}

/**
 * Used to geographically group premises
 */
@Entity
@Table(name = "ap_areas")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
data class ApAreaEntity(
  @Id
  val id: UUID,
  val name: String,
  val identifier: String,
  /**
   * Used to determine a user's [Cas1CruManagementAreaEntity] if no override is specified
   *
   * Also used to determine which management area an application is linked to on submission,
   * based upon the user's AP Area
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "default_cru1_management_area_id")
  val defaultCruManagementArea: Cas1CruManagementAreaEntity,
)
