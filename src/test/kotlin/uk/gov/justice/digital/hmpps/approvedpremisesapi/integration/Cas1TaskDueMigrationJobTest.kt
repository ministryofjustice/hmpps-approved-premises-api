package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas1TaskDueMigrationJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var cas1TaskDeadlineService: Cas1TaskDeadlineService

  @Test
  fun `it updates all tasks without a due date`() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    val assessments = List(10) { createAssessment() }
    val placementRequests = List(5) { createPlacementRequest() }
    val placementApplications = List(2) { createPlacementApplication() }

    migrationJobService.runMigrationJob(MigrationJobType.updateTaskDueDates)

    assessments.forEach {
      val updatedAssessment = assessmentTestRepository.findByIdOrNull(it.id)!!
      assertThat(updatedAssessment.dueAt).isNotNull()
      assertThat(updatedAssessment.dueAt?.roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .isEqualTo(cas1TaskDeadlineService.getDeadline(it)?.roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }

    placementRequests.forEach {
      val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(it.id)!!
      assertThat(updatedPlacementRequest.dueAt).isNotNull()
      assertThat(updatedPlacementRequest.dueAt?.roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .isEqualTo(cas1TaskDeadlineService.getDeadline(it).roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }

    placementApplications.forEach {
      val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(it.id)!!
      assertThat(updatedPlacementApplication.dueAt).isNotNull()
      assertThat(updatedPlacementApplication.dueAt?.roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .isEqualTo(cas1TaskDeadlineService.getDeadline(it).roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }
  }

  private fun createPlacementApplication(): PlacementApplicationEntity {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(givenAProbationRegion())
    }

    val assessment = createAssessment()
    val application = assessment.application as ApprovedPremisesApplicationEntity

    return placementApplicationFactory
      .produceAndPersist {
        withApplication(application)
        withCreatedByUser(user)
        withSubmittedAt(OffsetDateTime.now())
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
      withProbationRegion(givenAProbationRegion())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
    }

    return approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withDueAt(null)
    }
  }
}
