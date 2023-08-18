package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given Some Offenders`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.util.*

class FetchOffenderNamesForApplicationsFromCommunityApiJobTest : MigrationJobTestBase() {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Test
  fun `Any missing names are fetched from Community API with a 500ms artificial delay`() {
    `Given a User` { user, _ ->
      `Given Some Offenders` { offenderSequence ->
        val offenders = offenderSequence.take(5).toList()
        val schema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()

        val applications = offenders.map { offenderDetails: Pair<OffenderDetailSummary, InmateDetail> ->
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withName(null)
            withCrn(offenderDetails.first.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(schema)
          }
        }

        val applicationWithName = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withName("Some Name")
          withCreatedByUser(user)
          withApplicationSchema(schema)
        }

        migrationJobService.runMigrationJob(MigrationJobType.fetchOffenderNamesForApplications, 1)

        applications.forEachIndexed { index, application ->
          val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
          assertThat(updatedApplication.name).isEqualTo("${offenders[index].first.firstName.uppercase()} ${offenders[index].first.surname.uppercase()}")
        }

        val reloadedApplicationWithName = approvedPremisesApplicationRepository.findByIdOrNull(applicationWithName.id)!!
        assertThat(reloadedApplicationWithName.name).isEqualTo("Some Name")
      }
    }
  }
}
