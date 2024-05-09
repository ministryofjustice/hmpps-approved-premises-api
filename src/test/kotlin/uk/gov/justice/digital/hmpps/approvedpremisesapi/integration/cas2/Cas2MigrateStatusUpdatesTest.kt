package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 Assessor`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 POM User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2MigrateStatusUpdatesTest : MigrationJobTestBase() {

  @SpykBean
  lateinit var realStatusUpdateRepository: Cas2StatusUpdateRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realStatusUpdateRepository)
  }

  @Test
  fun `Should add assessments for CAS2 Status Updates that don't have one and returns 202 response`() {
    `Given a CAS2 POM User` { userEntity, _ ->
      `Given a CAS2 Assessor` { assessor, _ ->
        val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        // set up the application and assessment that we check
        val submittedApplication = createApplicationEntity(applicationSchema, userEntity, OffsetDateTime.now().minusDays(1))

        val assessment = cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(submittedApplication)
        }

        // create the status updates so there are two pages
        val assessmentIds = mutableListOf<UUID>()
        repeat(12) {
          assessmentIds.add(
            cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(submittedApplication)
              withAssessor(assessor)
            }.id,
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas2StatusUpdatesWithAssessments, 10)

        assessmentIds.forEach {
          val updatedStatusUpdate = realStatusUpdateRepository.findById(it)
          Assertions.assertThat(updatedStatusUpdate.get().assessment!!.id).isEqualTo(assessment.id)
        }
      }
    }
  }

  private fun createApplicationEntity(applicationSchema: Cas2ApplicationJsonSchemaEntity, userEntity: NomisUserEntity, submittedAt: OffsetDateTime?) =
    cas2ApplicationEntityFactory.produceAndPersist {
      withId(UUID.randomUUID())
      withApplicationSchema(applicationSchema)
      withCreatedByUser(userEntity)
      withData("{}")
      withSubmittedAt(submittedAt)
    }
}
