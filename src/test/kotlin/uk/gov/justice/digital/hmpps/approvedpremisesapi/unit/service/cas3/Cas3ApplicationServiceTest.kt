package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ApplicationService
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ApplicationServiceTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()

  private val mockUserAccessService = mockk<UserAccessService>()

  private val mockUserService = mockk<UserService>()

  private val mockCas3DomainEventService = mockk<uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService>()

  private val cas3ApplicationService = Cas3ApplicationService(mockApplicationRepository, mockUserService, mockUserAccessService, mockCas3DomainEventService)

  val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  val submittedApplication = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(user)
    .withProbationRegion(user.probationRegion)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  val inProgressApplication = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(user)
    .withProbationRegion(user.probationRegion)
    .withSubmittedAt(null)
    .produce()

  @Nested
  inner class SoftDeleteCas3Application {

    @Test
    fun `softDeleteCas3Application returns NotFound if Application does not exist`() {
      val applicationId = UUID.fromString("6504a40f-e52e-4f6b-b340-f87b480bf41d")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanAccessTemporaryAccommodationApplication(any(), any()) } returns true

      val result = cas3ApplicationService.markApplicationAsDeleted(applicationId)

      assertThat(result is CasResult.NotFound).isTrue

      verify(exactly = 0) {
        mockCas3DomainEventService.saveDraftReferralDeletedEvent(inProgressApplication, user)
      }
    }

    @Test
    fun `softDeleteCas3Application returns GeneralValidationError if Application is already submitted`() {
      every { mockApplicationRepository.findByIdOrNull(submittedApplication.id) } returns submittedApplication
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanAccessTemporaryAccommodationApplication(user, submittedApplication) } returns true

      val result = cas3ApplicationService.markApplicationAsDeleted(submittedApplication.id)

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val generalValidationError = (result as CasResult.GeneralValidationError).message

      assertThat(generalValidationError).isEqualTo("Cannot mark as deleted: temporary accommodation application already submitted.")

      verify(exactly = 0) {
        mockCas3DomainEventService.saveDraftReferralDeletedEvent(inProgressApplication, user)
      }
    }

    @Test
    fun `softDelete inProgress cas3 application returns success`() {
      every { mockApplicationRepository.findByIdOrNull(inProgressApplication.id) } returns inProgressApplication
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanAccessTemporaryAccommodationApplication(user, inProgressApplication) } returns true
      every { mockApplicationRepository.saveAndFlush(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      every { mockCas3DomainEventService.saveDraftReferralDeletedEvent(inProgressApplication, user) } just Runs

      val result = cas3ApplicationService.markApplicationAsDeleted(inProgressApplication.id)

      assertThat(result is CasResult.Success).isTrue

      verify {
        mockApplicationRepository.saveAndFlush(
          match {
            it.id == inProgressApplication.id
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveDraftReferralDeletedEvent(inProgressApplication, user)
      }
    }
  }
}
