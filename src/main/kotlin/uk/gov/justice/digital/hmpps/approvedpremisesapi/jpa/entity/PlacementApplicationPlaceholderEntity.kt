package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1BackfillAutomaticPlacementApplicationsJob
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
 * This table only exists to provide us with a unique ID in the requests for placements
 * report
 *
 * For new applications these entries are archived on application approval,
 * because we then have a corresponding placement_applications(automatic=true)
 * entry that can be used in reports instead
 *
 * They remain unarchived for application rejection because there is no corresponding
 * placement_applications(automatic=true) we can use in reports
 *
 * For older applications where we don't have a corresponding placement_applications(automatic=true),
 * on approval they remain unarchived so they continue to appear in reports
 *
 * All of the above issues will be fixed via [Cas1BackfillAutomaticPlacementApplicationsJob] at
 * which point this table could be used for other purposes (e.g. if we want to include
 * requests for placements in non-approved applications when listing RfPS in the
 * Cas1RequestForPlacementService)
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
