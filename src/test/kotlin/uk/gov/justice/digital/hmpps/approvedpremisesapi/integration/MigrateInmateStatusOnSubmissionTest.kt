package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockNotFoundPrisonTimeLineCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockServerErrorPrisonTimeLineCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulPrisonTimeLineCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonerInPrisonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.SignificantMovements
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MigrateInmateStatusOnSubmissionTest : MigrationJobTestBase() {

  @Test
  fun `Should ignore unsubmitted application`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(null)
        }

        assertInOutStatusIsNull(application)

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatusIsNull(application)
      }
    }
  }

  @Test
  fun `Should ignore submitted applications that already have an in out status`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withInmateInOutStatusOnSubmission("some value")
        }

        assertInOutStatus(application, "some value")

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatus(application, "some value")
      }
    }
  }

  @Test
  fun `Should update submitted application without an in out status set and NOMS number using prison timeline to IN`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val submissionDate = OffsetDateTime.of(2023, 4, 5, 12, 55, 0, 0, ZoneOffset.UTC)

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(submissionDate)
          withInmateInOutStatusOnSubmission(null)
        }

        assertInOutStatusIsNull(application)

        PrisonAPI_mockSuccessfulPrisonTimeLineCall(
          application.nomsNumber!!,
          PrisonerInPrisonSummary(
            listOf(
              PrisonPeriod(
                entryDate = LocalDateTime.of(2023, 3, 1, 12, 14, 0),
                releaseDate = LocalDateTime.of(2023, 5, 1, 12, 14, 0),
                movementDates = listOf(
                  SignificantMovements(
                    dateInToPrison = LocalDateTime.of(2023, 3, 1, 12, 14, 0),
                    dateOutOfPrison = LocalDateTime.of(2023, 5, 1, 12, 14, 0),
                  ),
                ),
              ),

            ),
          ),
        )

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatus(application, InOutStatus.IN.name)
      }
    }
  }

  @Test
  fun `Should update submitted application without an in out status set and NOMS number using prison timeline to OUT`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val submissionDate = OffsetDateTime.of(2023, 4, 5, 12, 55, 0, 0, ZoneOffset.UTC)

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(submissionDate)
          withInmateInOutStatusOnSubmission(null)
        }

        assertInOutStatusIsNull(application)

        PrisonAPI_mockSuccessfulPrisonTimeLineCall(
          application.nomsNumber!!,
          PrisonerInPrisonSummary(
            listOf(
              PrisonPeriod(
                entryDate = LocalDateTime.of(2023, 5, 1, 12, 14, 0),
                releaseDate = LocalDateTime.of(2023, 6, 1, 12, 14, 0),
                movementDates = listOf(
                  SignificantMovements(
                    dateInToPrison = LocalDateTime.of(2023, 5, 1, 12, 14, 0),
                    dateOutOfPrison = LocalDateTime.of(2023, 6, 1, 12, 14, 0),
                  ),
                ),
              ),

            ),
          ),
        )

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatus(application, InOutStatus.OUT.name)
      }
    }
  }

  @Test
  fun `Should update submitted application without an in out status set and no NOMS number to IN`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val submissionDate = OffsetDateTime.of(2023, 4, 5, 12, 55, 0, 0, ZoneOffset.UTC)

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(submissionDate)
          withInmateInOutStatusOnSubmission(null)
          withNomsNumber(null)
        }

        assertInOutStatusIsNull(application)

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatus(application, InOutStatus.IN.name)
      }
    }
  }

  @Test
  fun `Should update submitted application without an in out status set and no prison timeline available (404) to IN`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val submissionDate = OffsetDateTime.of(2023, 4, 5, 12, 55, 0, 0, ZoneOffset.UTC)

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(submissionDate)
          withInmateInOutStatusOnSubmission(null)
        }

        assertInOutStatusIsNull(application)

        PrisonAPI_mockNotFoundPrisonTimeLineCall(application.nomsNumber!!)

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatus(application, InOutStatus.IN.name)
      }
    }
  }

  @Test
  fun `Should not update submitted application without an in out status set and server error getting prison timeline`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val submissionDate = OffsetDateTime.of(2023, 4, 5, 12, 55, 0, 0, ZoneOffset.UTC)

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(submissionDate)
          withInmateInOutStatusOnSubmission(null)
        }

        assertInOutStatusIsNull(application)

        PrisonAPI_mockServerErrorPrisonTimeLineCall(application.nomsNumber!!)

        migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

        assertInOutStatusIsNull(application)
      }
    }
  }

  @Test
  fun `Should update submitted applications without in out status set, across multiple pages`() {
    val startingSubmissionDate = OffsetDateTime.of(2023, 4, 5, 12, 55, 0, 0, ZoneOffset.UTC)

    val applicationsToUpdate = mutableListOf<ApprovedPremisesApplicationEntity>()

    (1..31).forEach { i ->
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val submissionDate = startingSubmissionDate.plusDays(i.toLong())

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withCrn(offenderDetails.otherIds.crn)
            withConvictionId(12345)
            withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
            withSubmittedAt(submissionDate)
            withInmateInOutStatusOnSubmission(null)
          }

          PrisonAPI_mockSuccessfulPrisonTimeLineCall(
            application.nomsNumber!!,
            PrisonerInPrisonSummary(
              listOf(
                PrisonPeriod(
                  entryDate = submissionDate.minusDays(10).toLocalDateTime(),
                  releaseDate = submissionDate.plusDays(10).toLocalDateTime(),
                  movementDates = listOf(
                    SignificantMovements(
                      dateInToPrison = submissionDate.minusDays(10).toLocalDateTime(),
                      dateOutOfPrison = submissionDate.plusDays(10).toLocalDateTime(),
                    ),
                  ),
                ),

              ),
            ),
          )

          applicationsToUpdate.add(application)
        }
      }
    }

    migrationJobService.runMigrationJob(MigrationJobType.inmateStatusOnSubmission)

    applicationsToUpdate.forEach { application ->
      assertInOutStatus(application, InOutStatus.IN.name)
    }
  }

  private fun assertInOutStatusIsNull(
    applicationToCheck: ApprovedPremisesApplicationEntity,
  ) {
    val application = approvedPremisesApplicationRepository.findById(applicationToCheck.id)
    Assertions.assertThat(application).isNotNull()
    Assertions.assertThat(application.get().inmateInOutStatusOnSubmission).isNull()
  }

  private fun assertInOutStatus(
    applicationToCheck: ApprovedPremisesApplicationEntity,
    inmateInOutStatusOnSubmission: String,
  ) {
    val application = approvedPremisesApplicationRepository.findById(applicationToCheck.id)
    Assertions.assertThat(application).isNotNull()
    Assertions.assertThat(application.get().inmateInOutStatusOnSubmission).isEqualTo(inmateInOutStatusOnSubmission)
  }
}
