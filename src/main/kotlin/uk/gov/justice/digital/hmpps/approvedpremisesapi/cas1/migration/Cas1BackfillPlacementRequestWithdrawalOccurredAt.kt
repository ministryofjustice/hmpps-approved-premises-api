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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.util.UUID

@Component
class Cas1BackfillPlacementRequestWithdrawalOccurredAt(
  private val repository: Cas1BackfillPlacementRequestWithdrawalOccurredAtRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    repository.updateWithdrawalOccurredAtFromDomainEvents().let {
      migrationLogger.info("Have set withdrawal occurred at for $it placement requests")
    }
  }
}

@Repository
interface Cas1BackfillPlacementRequestWithdrawalOccurredAtRepository : JpaRepository<PlacementRequestEntity, UUID> {
  @QueryHints(QueryHint(name = "javax.persistence.query.timeout", value = "240000"))
  @Query(
    value = """
      UPDATE placement_requests pr
      SET withdrawal_occurred_at = latest_withdrawal.occurred_at
      FROM (
        SELECT DISTINCT ON (d.data -> 'eventDetails' ->> 'matchRequestId')
          d.data -> 'eventDetails' ->> 'matchRequestId' AS placement_request_id,
          d.occurred_at
        FROM domain_events AS d
        WHERE d.type = 'APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN'
        ORDER BY d.data -> 'eventDetails' ->> 'matchRequestId', d.created_at DESC
      ) AS latest_withdrawal
      WHERE latest_withdrawal.placement_request_id = CAST(pr.id AS text)
        AND pr.is_withdrawn IS TRUE
        AND pr.withdrawal_occurred_at IS NULL;
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updateWithdrawalOccurredAtFromDomainEvents(): Int
}
