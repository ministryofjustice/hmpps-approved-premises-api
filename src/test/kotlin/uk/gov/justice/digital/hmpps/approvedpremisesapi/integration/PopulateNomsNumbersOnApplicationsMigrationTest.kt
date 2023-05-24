package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall

class PopulateNomsNumbersOnApplicationsMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All Applications without Noms Number have Noms Number populated from Community API with a 500ms artificial delay`() {
    `Given a User` { createdByUser, _ ->
      val applicationJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()

      val applicationOne = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(createdByUser)
        withApplicationSchema(applicationJsonSchema)
        withCrn("CRNAPPLICATION1")
        withNomsNumber(null)
      }

      val applicationTwo = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(createdByUser)
        withApplicationSchema(applicationJsonSchema)
        withCrn("CRNAPPLICATION2")
        withNomsNumber(null)
      }

      CommunityAPI_mockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(applicationOne.crn)
          .withNomsNumber("NOMSAPPLICATION1")
          .produce()
      )

      CommunityAPI_mockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(applicationTwo.crn)
          .withNomsNumber("NOMSAPPLICATION2")
          .produce()
      )

      val startTime = System.currentTimeMillis()
      migrationJobService.runMigrationJob(MigrationJobType.populateNomsNumberApplications)
      val endTime = System.currentTimeMillis()

      assertThat(endTime - startTime).isGreaterThan(500 * 2)

      val applicationOneAfterUpdate = approvedPremisesApplicationRepository.findByIdOrNull(applicationOne.id)!!
      val applicationTwoAfterUpdate = approvedPremisesApplicationRepository.findByIdOrNull(applicationTwo.id)!!

      assertThat(applicationOneAfterUpdate.nomsNumber).isEqualTo("NOMSAPPLICATION1")
      assertThat(applicationTwoAfterUpdate.nomsNumber).isEqualTo("NOMSAPPLICATION2")
    }
  }
}
