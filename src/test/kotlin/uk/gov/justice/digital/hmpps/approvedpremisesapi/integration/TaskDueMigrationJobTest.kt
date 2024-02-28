package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService
import java.time.OffsetDateTime
import java.util.UUID

class TaskDueMigrationJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var taskDeadlineService: TaskDeadlineService

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var placementApplicationSchema: ApprovedPremisesPlacementApplicationJsonSchemaEntity

  @BeforeEach
  fun setup() {
    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    placementApplicationSchema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
  }

  @Test
  fun `it updates all tasks without a due date`() {
    GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

    val assessments = List(10) { createAssessment() }
    val placementRequests = List(5) { createPlacementRequest() }
    val placementApplications = List(2) { createPlacementApplication() }

    migrationJobService.runMigrationJob(MigrationJobType.taskDueDates)

    assessments.forEach {
      val updatedAssessment = assessmentTestRepository.findByIdOrNull(it.id)!!
      assertThat(updatedAssessment.dueAt).isNotNull()
      assertThat(updatedAssessment.dueAt).isEqualTo(taskDeadlineService.getDeadline(it))
    }

    placementRequests.forEach {
      val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(it.id)!!
      assertThat(updatedPlacementRequest.dueAt).isNotNull()
      assertThat(updatedPlacementRequest.dueAt).isEqualTo(taskDeadlineService.getDeadline(it))
    }

    placementApplications.forEach {
      val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(it.id)!!
      assertThat(updatedPlacementApplication.dueAt).isNotNull()
      assertThat(updatedPlacementApplication.dueAt).isEqualTo(taskDeadlineService.getDeadline(it))
    }
  }

  private fun createPlacementApplication(): PlacementApplicationEntity {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
    }

    val assessment = createAssessment()
    val application = assessment.application as ApprovedPremisesApplicationEntity

    return placementApplicationFactory
      .produceAndPersist {
        withApplication(application)
        withCreatedByUser(user)
        withSchemaVersion(placementApplicationSchema)
        withDueAt(null)
      }
  }

  private fun createPlacementRequest(): PlacementRequestEntity {
    val assessment = createAssessment()
    val application = assessment.application as ApprovedPremisesApplicationEntity

    return placementRequestFactory.produceAndPersist {
      withAssessment(assessment)
      withApplication(application)
      withPlacementRequirements(
        placementRequirementsFactory.produceAndPersist {
          withApplication(application)
          withAssessment(assessment)
          withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
          withDesirableCriteria(
            characteristicEntityFactory.produceAndPersistMultiple(5),
          )
          withEssentialCriteria(
            characteristicEntityFactory.produceAndPersistMultiple(3),
          )
        },
      )
      withDueAt(null)
    }
  }

  private fun createAssessment(): ApprovedPremisesAssessmentEntity {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    return approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAssessmentSchema(assessmentSchema)
      withApplication(application)
      withDueAt(null)
    }
  }
}
