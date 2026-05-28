package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase

class Cas2BackfillApplicationCohortIntegrationTest : MigrationJobTestBase() {

  @Test
  fun `Successfully backfills application cohorts`() {
    givenACas2PomUser { userEntity, _ ->

      val courtBailApplications = cas2ApplicationEntityFactory.produceAndPersistMultiple(4) {
        withApplicationOrigin(ApplicationOrigin.courtBail)
        withCreatedByUser(userEntity)
      }

      val prisonBailApplications = cas2ApplicationEntityFactory.produceAndPersistMultiple(4) {
        withApplicationOrigin(ApplicationOrigin.prisonBail)
        withCreatedByUser(userEntity)
      }

      val hdcApplications = cas2ApplicationEntityFactory.produceAndPersistMultiple(4) {
        withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
        withCreatedByUser(userEntity)
      }

      val courtBailApplicationWithCohort = cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationOrigin(ApplicationOrigin.courtBail)
        withCreatedByUser(userEntity)
        withCohort(Cas2Cohort.COURT_BAIL)
      }

      val prisonBailApplicationWithCohort = cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationOrigin(ApplicationOrigin.prisonBail)
        withCreatedByUser(userEntity)
        withCohort(Cas2Cohort.PRISON_BAIL)
      }

      val hdcApplicationWithCohort = cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
        withCreatedByUser(userEntity)
        withCohort(Cas2Cohort.HDC)
      }

      assertThat(courtBailApplications.map { it.cohort }).allMatch { it == null }
      assertThat(prisonBailApplications.map { it.cohort }).allMatch { it == null }
      assertThat(hdcApplications.map { it.cohort }).allMatch { it == null }

      migrationJobService.runMigrationJob(MigrationJobType.cas2BackfillApplicationCohorts)

      val updatedCourtBailApplications = cas2ApplicationRepository.findAllById(courtBailApplications.map { it.id })
      val updatedPrisonBailApplications = cas2ApplicationRepository.findAllById(prisonBailApplications.map { it.id })
      val updatedHdcApplications = cas2ApplicationRepository.findAllById(hdcApplications.map { it.id })

      val savedCourtBailApplicationWithCohort = cas2ApplicationRepository.findById(courtBailApplicationWithCohort.id).get()
      val savedPrisonBailApplicationWithCohort = cas2ApplicationRepository.findById(prisonBailApplicationWithCohort.id).get()
      val savedHdcApplicationWithCohort = cas2ApplicationRepository.findById(hdcApplicationWithCohort.id).get()

      assertThat(updatedCourtBailApplications.map { it.cohort }).allMatch { it == Cas2Cohort.COURT_BAIL }
      assertThat(updatedPrisonBailApplications.map { it.cohort }).allMatch { it == Cas2Cohort.PRISON_BAIL }
      assertThat(updatedHdcApplications.map { it.cohort }).allMatch { it == Cas2Cohort.HDC }
      assertThat(savedCourtBailApplicationWithCohort.cohort).isEqualTo(Cas2Cohort.COURT_BAIL)
      assertThat(savedPrisonBailApplicationWithCohort.cohort).isEqualTo(Cas2Cohort.PRISON_BAIL)
      assertThat(savedHdcApplicationWithCohort.cohort).isEqualTo(Cas2Cohort.HDC)
    }
  }

  @Test
  fun `Raises exception if application origin is other`() {
    givenACas2PomUser { userEntity, _ ->
      cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationOrigin(ApplicationOrigin.other)
        withCreatedByUser(userEntity)
      }

      migrationJobService.runMigrationJob(MigrationJobType.cas2BackfillApplicationCohorts)

      assertThat(logEntries.mapNotNull { it.throwable?.message }).anyMatch { it.contains("Unexpected application origin: other") }
    }
  }
}
