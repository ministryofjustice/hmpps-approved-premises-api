package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`

class FetchOffenderNamesForApplicationsFromCommunityApiJobTest : MigrationJobTestBase() {
  @Test
  fun `Any missing names are fetched from Community API with a 500ms artificial delay`() {
    `Given a User` { user, _ ->
      `Given an Offender` { offenderDetailsOne, _ ->
        `Given an Offender` { offenderDetailsTwo, _ ->
          val schema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val applicationOne = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withName(null)
            withCrn(offenderDetailsOne.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(schema)
          }

          val applicationTwo = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withName(null)
            withCrn(offenderDetailsTwo.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(schema)
          }

          val startTime = System.currentTimeMillis()
          migrationJobService.runMigrationJob(MigrationJobType.fetchOffenderNamesForApplications)
          val endTime = System.currentTimeMillis()

          assertThat(endTime - startTime).isGreaterThan(500 * 2)

          val applicationOneAfterUpdate = approvedPremisesApplicationRepository.findByIdOrNull(applicationOne.id)!!
          val applicationTwoAfterUpdate = approvedPremisesApplicationRepository.findByIdOrNull(applicationTwo.id)!!

          assertThat(applicationOneAfterUpdate.name).isEqualTo("${offenderDetailsOne.firstName.uppercase()} ${offenderDetailsOne.surname.uppercase()}")
          assertThat(applicationTwoAfterUpdate.name).isEqualTo("${offenderDetailsTwo.firstName.uppercase()} ${offenderDetailsTwo.surname.uppercase()}")
        }
      }
    }
  }
}
