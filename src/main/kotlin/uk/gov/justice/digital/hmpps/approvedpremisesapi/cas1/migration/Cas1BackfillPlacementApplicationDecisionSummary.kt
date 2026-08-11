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
class Cas1BackfillPlacementApplicationDecisionSummary(
  private val repository: Cas1BackfillPlacementApplicationDecisionSummaryRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    repository.updateDecisionSummaryFromDomainEvents().let {
      migrationLogger.info("Have set decision summary for $it placement applications")
    }
  }
}

@Repository
interface Cas1BackfillPlacementApplicationDecisionSummaryRepository : JpaRepository<PlacementApplicationEntity, UUID> {
  @QueryHints(QueryHint(name = "javax.persistence.query.timeout", value = "240000"))
  @Query(
    value = """
      with latest_event as (
          select distinct on (d.data -> 'eventDetails' ->> 'placementApplicationId')
              d.data -> 'eventDetails' ->> 'placementApplicationId' as placement_application_id,
              d.data -> 'eventDetails' ->> 'decisionSummary' as decision_summary
          from domain_events as d
          where d.type = 'APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED'
          order by d.data -> 'eventDetails' ->> 'placementApplicationId', d.created_at desc
      ),
      to_update as (
          select
              pa.id as id,
              latest_event.decision_summary as decision_summary
          from placement_applications pa
          inner join latest_event on latest_event.placement_application_id = CAST(pa.id AS text)
          where pa.decision IS NOT NULL
          and pa.decision_summary IS NULL
          and latest_event.decision_summary IS NOT NULL
      )
      UPDATE placement_applications
      SET decision_summary = to_update.decision_summary
      FROM to_update
      WHERE placement_applications.id = to_update.id;
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updateDecisionSummaryFromDomainEvents(): Int
}
