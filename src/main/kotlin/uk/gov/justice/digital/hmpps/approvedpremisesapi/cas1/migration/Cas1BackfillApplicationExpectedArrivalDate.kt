package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import java.util.UUID

@Component
class Cas1BackfillApplicationExpectedArrivalDate(
  private val repository: Cas1BackfillApplicationExpectedArrivalDateRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    val updated = repository.updateExpectedArrivalDate()
    migrationLogger.info("Have set expected arrival date for $updated applications")
  }
}

@Repository
interface Cas1BackfillApplicationExpectedArrivalDateRepository : JpaRepository<ApprovedPremisesApplicationEntity, UUID> {
  @Modifying
  @Query(
    value = """
      UPDATE approved_premises_applications
      SET expected_arrival_date = CAST(arrival_date AT TIME ZONE 'UTC' AS date)
      WHERE expected_arrival_date IS NULL
        AND arrival_date IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun updateExpectedArrivalDate(): Int
}
