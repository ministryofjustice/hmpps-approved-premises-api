package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface PlacementApplicationAutomaticRepository : JpaRepository<PlacementApplicationAutomaticEntity, UUID>

/**
 * Used to capture requests for placements implicit in the original applications
 * i.e. where an arrival date is defined in the original application
 *
 * This is a stop-gap solution to support reporting until we fix the
 * bifurcated request for placement model
 *
 * See [PlacementRequestEntity.isForApplicationsArrivalDate] for more information
 */
@Entity
@Table(name = "placement_applications_automatic")
data class PlacementApplicationAutomaticEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  val submittedAt: OffsetDateTime,
  val expectedArrivalDate: OffsetDateTime,
)
