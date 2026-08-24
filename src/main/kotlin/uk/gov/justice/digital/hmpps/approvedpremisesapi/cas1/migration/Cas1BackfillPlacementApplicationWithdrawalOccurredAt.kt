package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import java.util.UUID

@Component
class Cas1BackfillPlacementApplicationWithdrawalOccurredAt(
  private val repository: Cas1BackfillPlacementApplicationWithdrawalOccurredAtRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    repository.updateWithdrawalOccurredAtFromDomainEvents().let {
      migrationLogger.info("Have set withdrawal occurred at for $it placement applications")
    }
  }
}

@Repository
interface Cas1BackfillPlacementApplicationWithdrawalOccurredAtRepository : JpaRepository<PlacementApplicationEntity, UUID> {
  @QueryHints(QueryHint(name = "javax.persistence.query.timeout", value = "240000"))
  @Query(
    value = """
      UPDATE placement_applications pa
      SET withdrawal_occurred_at = latest_withdrawal.occurred_at
      FROM (
        SELECT DISTINCT ON (d.data -> 'eventDetails' ->> 'placementApplicationId')
          d.data -> 'eventDetails' ->> 'placementApplicationId' AS placement_application_id,
          d.occurred_at
        FROM domain_events AS d
        WHERE d.type = 'APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN'
        ORDER BY d.data -> 'eventDetails' ->> 'placementApplicationId', d.created_at DESC
      ) AS latest_withdrawal
      WHERE latest_withdrawal.placement_application_id = CAST(pa.id AS text)
        AND pa.is_withdrawn IS TRUE
        AND pa.withdrawal_occurred_at IS NULL;
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updateWithdrawalOccurredAtFromDomainEvents(): Int
}
