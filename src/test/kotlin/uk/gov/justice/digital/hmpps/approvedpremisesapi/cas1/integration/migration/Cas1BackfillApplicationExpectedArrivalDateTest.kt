package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1BackfillApplicationExpectedArrivalDateTest : MigrationJobTestBase() {
  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @ParameterizedTest
  @ValueSource(strings = ["America/Los_Angeles", "Pacific/Auckland", "Europe/London"])
  fun `backfills UTC dates without overwriting existing values and can be rerun`(timeZone: String) {
    val (user) = givenAUser()
    val applicationsToBackfill = listOf(
      "2026-01-01T00:00:00Z" to "2026-01-01",
      "2026-07-01T00:00:00Z" to "2026-07-01",
      "2024-02-29T23:30:00Z" to "2024-02-29",
      "2026-01-01T00:30:00+02:00" to "2025-12-31",
      "2026-01-01T00:59:00+01:00" to "2025-12-31",
    ).map { (arrival, expected) ->
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withArrivalDate(OffsetDateTime.parse(arrival))
      } to LocalDate.parse(expected)
    }
    val existingDate = LocalDate.parse("2026-08-03")
    val applicationWithExistingDate = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withArrivalDate(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
      withExpectedArrivalDate(existingDate)
    }
    val applicationWithoutArrivalDate = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withArrivalDate(null)
    }
    val applicationWithOnlyExpectedDate = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withArrivalDate(null)
      withExpectedArrivalDate(existingDate)
    }

    transactionTemplate.executeWithoutResult {
      jdbcTemplate.execute("SET LOCAL TIME ZONE '$timeZone'")
      migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillApplicationExpectedArrivalDate)
      migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillApplicationExpectedArrivalDate)
    }

    applicationsToBackfill.forEach { (application, expectedDate) ->
      val updated = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
      assertThat(updated.expectedArrivalDate).isEqualTo(expectedDate)
      assertThat(updated.arrivalDate).isEqualTo(application.arrivalDate)
    }
    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationWithExistingDate.id)!!.expectedArrivalDate)
      .isEqualTo(existingDate)
    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationWithoutArrivalDate.id)!!.expectedArrivalDate)
      .isNull()
    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationWithOnlyExpectedDate.id)!!.expectedArrivalDate)
      .isEqualTo(existingDate)
    assertThat(logEntries.none { it.level == "error" }).isTrue()
    assertThat(logEntries.any { it.message == "Have set expected arrival date for 5 applications" }).isTrue()
    assertThat(logEntries.any { it.message == "Have set expected arrival date for 0 applications" }).isTrue()
  }
}
