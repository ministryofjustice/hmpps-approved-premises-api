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
interface PlacementApplicationPlaceholderRepository : JpaRepository<PlacementApplicationPlaceholderEntity, UUID> {
  fun findByApplication(application: ApplicationEntity): PlacementApplicationPlaceholderEntity?
  fun findByApplicationAndArchivedIsFalse(application: ApplicationEntity): PlacementApplicationPlaceholderEntity?
}

/**
 * Used to capture requests for placements made in the original applications
 * i.e. where an arrival date is defined in the original application
 *
 * For new applications these entries are archived on application submission,
 * because we then have a corresponding entry into placement_applications[automatic=true]
 *
 * For older applications where we weren't creating a placement_applications[automatic=true],
 * these are not archived
 *
 * This table only exists to provide us with a unique ID in the requests for placements
 * report.
 *
 * See [PlacementRequestEntity.isForLegacyInitialRequestForPlacement] for more information
 */
@Entity
@Table(name = "placement_applications_placeholder")
data class PlacementApplicationPlaceholderEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  val submittedAt: OffsetDateTime,
  val expectedArrivalDate: OffsetDateTime,
  var archived: Boolean = false,
)
