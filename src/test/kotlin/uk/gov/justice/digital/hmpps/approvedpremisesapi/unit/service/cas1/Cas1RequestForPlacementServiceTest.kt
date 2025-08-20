package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas1RequestForPlacementServiceTest {
  private val applicationService = mockk<ApplicationService>()
  private val cas1PlacementApplicationService = mockk<Cas1PlacementApplicationService>()
  private val placementRequestService = mockk<Cas1PlacementRequestService>()
  private val requestForPlacementTransformer = mockk<RequestForPlacementTransformer>()
  private val cas1WithdrawableService = mockk<Cas1WithdrawableService>()

  private val cas1RequestForPlacementService = Cas1RequestForPlacementService(
    applicationService,
    cas1PlacementApplicationService,
    placementRequestService,
    requestForPlacementTransformer,
    cas1WithdrawableService,
  )

  @BeforeEach
  fun setupRequestForPlacementTransformerMock() {
    every { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(any(), any()) } returns mockRfp()
    every { requestForPlacementTransformer.transformPlacementRequestEntityToApi(any(), any()) } returns mockRfp()
  }

  private fun mockRfp(): RequestForPlacement {
    val mock = mockk<RequestForPlacement>()
    every { mock.createdAt } returns Instant.now()
    return mock
  }

  companion object {
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()
  }

  @Nested
  inner class GetRequestsForPlacementByApplication {
    @Test
    fun `Returns NotFound result if no application with the specified ID was found`() {
      every { applicationService.getApplication(any()) } returns null

      val id = UUID.randomUUID()
      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(id, user)

      assertThatCasResult(result).isNotFound("Application", id)
    }

    @Test
    fun `Returns all placement applications attached to the application with the specified ID`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplications = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produceMany()
        .take(5)
        .toList()

      every { applicationService.getApplication(application.id) } returns application

      every {
        cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
      } returns placementApplications

      placementApplications.forEach {
        every { cas1WithdrawableService.isDirectlyWithdrawable(it, user) } returns true
      }

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns listOf()

      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, user)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).hasSize(placementApplications.size)
      }

      placementApplications.forEach {
        verify(exactly = 1) { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(it, true) }
      }
    }

    @Test
    fun `Returns all placement requests attached to the application with the specified ID`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementRequests = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .produceMany()
        .take(5)
        .toList()

      every { applicationService.getApplication(application.id) } returns application

      every {
        cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
      } returns listOf()

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns placementRequests

      placementRequests.forEach {
        every { cas1WithdrawableService.isDirectlyWithdrawable(it, user) } returns true
      }

      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, user)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).hasSize(placementRequests.size)
      }

      placementRequests.forEach {
        verify(exactly = 1) { requestForPlacementTransformer.transformPlacementRequestEntityToApi(it, true) }
      }
    }
  }
}
