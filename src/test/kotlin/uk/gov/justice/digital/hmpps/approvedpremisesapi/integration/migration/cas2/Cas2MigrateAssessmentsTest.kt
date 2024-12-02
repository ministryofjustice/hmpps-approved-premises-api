package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2MigrateAssessmentsTest : MigrationJobTestBase() {

  @SpykBean
  lateinit var realAssessmentRepository: Cas2AssessmentRepository

  @SpykBean
  lateinit var realApplicationRepository: Cas2ApplicationRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realAssessmentRepository)
    clearMocks(realApplicationRepository)
  }

  @Test
  fun `Should create assessments for CAS2 Applications that are submitted and returns 202 response`() {
    givenACas2PomUser { userEntity, _ ->
      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      val submittedAt = OffsetDateTime.now().minusDays(1)

      val unsubmittedApp = createApplicationEntity(applicationSchema, userEntity, null)

      val submittedWithoutAssessment = createApplicationEntity(applicationSchema, userEntity, submittedAt)

      val submittedWithAssessment = createApplicationEntity(applicationSchema, userEntity, submittedAt)

      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(submittedWithAssessment)
      }

      migrationJobService.runMigrationJob(MigrationJobType.CAS2_APPLICATIONS_WITH_ASSESSMENTS, 1)

      checkUnsubmittedDoesNotHaveAssessment(unsubmittedApp)

      checkAssessmentWasCreated(submittedWithoutAssessment)

      checkApplicationHasAssociationWithAssessment(submittedWithoutAssessment)
    }
  }

  private fun checkApplicationHasAssociationWithAssessment(submittedWithoutAssessment: Cas2ApplicationEntity) {
    val application = realApplicationRepository.findById(submittedWithoutAssessment.id)
    Assertions.assertThat(application.get().assessment).isNotNull()
  }

  private fun checkAssessmentWasCreated(submittedWithoutAssessment: Cas2ApplicationEntity) {
    val newAssessment = realAssessmentRepository.findFirstByApplicationId(submittedWithoutAssessment.id)
    Assertions.assertThat(newAssessment).isNotNull()
  }

  private fun checkUnsubmittedDoesNotHaveAssessment(unsubmittedApp: Cas2ApplicationEntity) {
    val unsubmittedApplication = realApplicationRepository.findById(unsubmittedApp.id)
    Assertions.assertThat(unsubmittedApplication.get().assessment).isNull()
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
