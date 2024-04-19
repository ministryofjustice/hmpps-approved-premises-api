package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class RequestForPlacementServiceTest {
  private val applicationService = mockk<ApplicationService>()
  private val placementApplicationService = mockk<PlacementApplicationService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val requestForPlacementTransformer = mockk<RequestForPlacementTransformer>()

  private val requestForPlacementService = RequestForPlacementService(
    applicationService,
    placementApplicationService,
    placementRequestService,
    requestForPlacementTransformer,
  )

  @BeforeEach
  fun setupRequestForPlacementTransformerMock() {
    every { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(any()) } returns mockk()
    every { requestForPlacementTransformer.transformPlacementRequestEntityToApi(any()) } returns mockk()
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

      val result = requestForPlacementService.getRequestsForPlacementByApplication(UUID.randomUUID())

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
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
        placementApplicationService.getAllPlacementApplicationsForApplicationId(application.id)
      } returns placementApplications

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns listOf()

      val result = requestForPlacementService.getRequestsForPlacementByApplication(application.id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).hasSize(placementApplications.size)
      placementApplications.forEach {
        verify(exactly = 1) { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(it) }
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
        placementApplicationService.getAllPlacementApplicationsForApplicationId(application.id)
      } returns listOf()

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns placementRequests

      val result = requestForPlacementService.getRequestsForPlacementByApplication(application.id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).hasSize(placementRequests.size)
      placementRequests.forEach {
        verify(exactly = 1) { requestForPlacementTransformer.transformPlacementRequestEntityToApi(it) }
      }
    }
  }

  @Nested
  inner class GetRequestForPlacement {
    @Test
    fun `Returns NotFound result if neither a placement application nor a placement request with the specified ID was found`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { placementApplicationService.getApplicationOrNull(any()) } returns null
      every { placementRequestService.getPlacementRequestOrNull(any()) } returns null

      val result = requestForPlacementService.getRequestForPlacement(application, UUID.randomUUID())

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns the placement application with the specified ID`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication
      every { placementRequestService.getPlacementRequestOrNull(any()) } returns null

      val result = requestForPlacementService.getRequestForPlacement(application, placementApplication.id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)

      verify(exactly = 1) { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication) }
    }

    @Test
    fun `Returns the placement request with the specified ID`() {
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

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .produce()

      every { placementApplicationService.getApplicationOrNull(any()) } returns null
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val result = requestForPlacementService.getRequestForPlacement(application, placementRequest.id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)

      verify(exactly = 1) { requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest) }
    }
  }
}
