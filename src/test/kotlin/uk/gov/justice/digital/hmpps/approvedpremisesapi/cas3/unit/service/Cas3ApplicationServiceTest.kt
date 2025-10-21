package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas3ApplicationServiceTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockLockableApplicationRepository = mockk<LockableApplicationRepository>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockUserService = mockk<UserService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockProbationRegionRepository = mockk<ProbationRegionRepository>()

  private val cas3ApplicationService = Cas3ApplicationService(
    mockApplicationRepository,
    mockLockableApplicationRepository,
    mockProbationDeliveryUnitRepository,
    mockUserService,
    mockUserAccessService,
    mockAssessmentService,
    mockCas3DomainEventService,
    mockObjectMapper,
    mockProbationRegionRepository,
  )

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
  inner class UpdateApplication {
    @Test
    fun `updateApplication returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      assertThat(
        cas3ApplicationService.updateApplication(
          applicationId = applicationId,
          data = "{}",
        ) is CasResult.NotFound,
      ).isTrue
    }

    @Test
    fun `updateApplication returns Unauthorised when application doesn't belong to request user`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          UserEntityFactory()
            .withProbationRegion(probationRegion)
            .produce()
        }
        .withProbationRegion(probationRegion)
        .produce()

      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockUserService.getUserForRequest() } returns UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      assertThat(
        cas3ApplicationService.updateApplication(
          applicationId = applicationId,
          data = "{}",
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `updateApplication returns GeneralValidationError when application has already been submitted`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val user = UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = cas3ApplicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError

      assertThat(result.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `updateApplication returns GeneralValidationError when application has already been deleted`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val user = UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withDeletedAt(OffsetDateTime.now().minusDays(7))
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = cas3ApplicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError

      assertThat(result.message).isEqualTo("This application has already been deleted")
    }

    @Test
    fun `updateApplication returns Success with updated Application`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val user = UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val updatedData = """
      {
        "aProperty": "value"
      }
    """

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val result = cas3ApplicationService.updateApplication(
        applicationId = applicationId,
        data = updatedData,
      )

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      val approvedPremisesApplication = result.value as TemporaryAccommodationApplicationEntity

      assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
    }
  }

  @SuppressWarnings("UnusedPrivateProperty")
  @Nested
  inner class ApplicationSubmission {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val user = UserEntityFactory()
      .withDeliusUsername(this.username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    private val submitApplication = Cas3SubmitApplication(
      translatedDocument = {},
      arrivalDate = LocalDate.now(),
      summaryData = {
        val num = 50
        val text = "Hello world!"
      },
      probationDeliveryUnitId = UUID.randomUUID(),
    )

    private val submitTemporaryAccommodationApplicationWithMiReportingData = Cas3SubmitApplication(
      translatedDocument = {},
      arrivalDate = LocalDate.now(),
      summaryData = {
        val num = 50
        val text = "Hello world!"
      },
      isRegisteredSexOffender = true,
      needsAccessibleProperty = true,
      hasHistoryOfArson = true,
      isDutyToReferSubmitted = true,
      dutyToReferSubmissionDate = LocalDate.now().minusDays(7),
      dutyToReferOutcome = "Accepted – Prevention/ Relief Duty",
      isApplicationEligible = true,
      eligibilityReason = "homelessFromApprovedPremises",
      dutyToReferLocalAuthorityAreaName = "Aberdeen City",
      personReleaseDate = LocalDate.now().plusDays(1),
      isHistoryOfSexualOffence = true,
      probationDeliveryUnitId = UUID.randomUUID(),
      isConcerningSexualBehaviour = true,
      isConcerningArsonBehaviour = true,
      prisonReleaseTypes = listOf(
        "Standard recall",
        "ECSL",
        "PSS",
      ),
    )

    @BeforeEach
    fun setup() {
      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockObjectMapper.writeValueAsString(submitApplication.translatedDocument) } returns "{}"
      every { mockObjectMapper.writeValueAsString(submitTemporaryAccommodationApplicationWithMiReportingData.translatedDocument) } returns "{}"
    }

    @Test
    fun `submitApplication returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        cas3ApplicationService.submitApplication(
          applicationId,
          submitApplication,
        ) is CasResult.NotFound,
      ).isTrue
    }

    @Test
    fun `submitApplication returns Unauthorised when application doesn't belong to request user`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockUserService.getUserForRequest() } returns UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      assertThat(
        cas3ApplicationService.submitApplication(
          applicationId,
          submitApplication,
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `submitApplication returns GeneralValidationError when application has already been submitted`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = cas3ApplicationService.submitApplication(
        applicationId,
        submitApplication,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val validatableActionResult = result as CasResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `submitApplication returns GeneralValidationError when application has already been deleted`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withDeletedAt(OffsetDateTime.now().minusDays(22))
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = cas3ApplicationService.submitApplication(
        applicationId,
        submitApplication,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val validatableActionResult = result as CasResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been deleted")
    }

    @Test
    fun `submitApplication returns Success and creates assessment`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockProbationDeliveryUnitRepository.findByIdOrNull(any()) } returns null
      every {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitApplication.summaryData!!,
        )
      } returns TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
        .produce()

      every { mockCas3DomainEventService.saveReferralSubmittedEvent(any()) } just Runs

      val result = cas3ApplicationService.submitApplication(
        applicationId,
        submitApplication,
      )

      assertThat(result is CasResult.Success).isTrue

      val validatableActionResult = result as CasResult.Success
      val persistedApplication = validatableActionResult.value as TemporaryAccommodationApplicationEntity
      assertThat(persistedApplication.arrivalDate).isEqualTo(
        OffsetDateTime.of(
          submitApplication.arrivalDate,
          LocalTime.MIDNIGHT,
          ZoneOffset.UTC,
        ),
      )

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitApplication.summaryData!!,
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveReferralSubmittedEvent(application)
      }
    }

    @Test
    fun `submitApplication records MI reporting data when supplied`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .withName(user.name)
        .produce()

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockProbationDeliveryUnitRepository.findByIdOrNull(any()) } returns null
      every {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplicationWithMiReportingData.summaryData!!,
        )
      } returns TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
        .produce()

      every { mockCas3DomainEventService.saveReferralSubmittedEvent(any()) } just Runs

      val result = cas3ApplicationService.submitApplication(
        applicationId,
        submitTemporaryAccommodationApplicationWithMiReportingData,
      )

      assertThat(result is CasResult.Success).isTrue

      val validatableActionResult = result as CasResult.Success
      val persistedApplication = validatableActionResult.value as TemporaryAccommodationApplicationEntity
      assertThat(persistedApplication.arrivalDate).isEqualTo(
        OffsetDateTime.of(
          submitApplication.arrivalDate,
          LocalTime.MIDNIGHT,
          ZoneOffset.UTC,
        ),
      )
      assertThat(persistedApplication.isRegisteredSexOffender).isEqualTo(true)
      assertThat(persistedApplication.needsAccessibleProperty).isEqualTo(true)
      assertThat(persistedApplication.hasHistoryOfArson).isEqualTo(true)
      assertThat(persistedApplication.isDutyToReferSubmitted).isEqualTo(true)
      assertThat(persistedApplication.dutyToReferSubmissionDate).isEqualTo(LocalDate.now().minusDays(7))
      assertThat(persistedApplication.isEligible).isEqualTo(true)
      assertThat(persistedApplication.eligibilityReason).isEqualTo("homelessFromApprovedPremises")
      assertThat(persistedApplication.dutyToReferLocalAuthorityAreaName).isEqualTo("Aberdeen City")
      assertThat(persistedApplication.personReleaseDate).isEqualTo(submitTemporaryAccommodationApplicationWithMiReportingData.personReleaseDate)
      assertThat(persistedApplication.probationDeliveryUnit?.id).isNull()
      assertThat(persistedApplication.name).isEqualTo(user.name)
      assertThat(persistedApplication.isHistoryOfSexualOffence).isEqualTo(true)
      assertThat(persistedApplication.isConcerningSexualBehaviour).isEqualTo(true)
      assertThat(persistedApplication.isConcerningArsonBehaviour).isEqualTo(true)
      assertThat(persistedApplication.dutyToReferOutcome).isEqualTo("Accepted – Prevention/ Relief Duty")
      assertThat(persistedApplication.prisonReleaseTypes).isEqualTo("Standard recall,ECSL,PSS")

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplicationWithMiReportingData.summaryData!!,
        )
      }
    }
  }

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

  @Nested
  inner class OutOfRegionFields {

    @Test
    fun `should persist and retrieve both out of region probation region and PDU fields when set`() {
      val outOfRegionProbationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val mainPdu = mockk<uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity>()
      every { mainPdu.id } returns UUID.randomUUID()
      every { mainPdu.name } returns "Main PDU"

      val outOfRegionPdu = mockk<uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity>()
      every { outOfRegionPdu.id } returns UUID.randomUUID()
      every { outOfRegionPdu.name } returns "Out Of Region PDU"

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withProbationDeliveryUnit(mainPdu)
        .withOutOfRegionProbationRegion(outOfRegionProbationRegion)
        .withoutOfRegionProbationDeliveryUnit(outOfRegionPdu)
        .produce()

      every { mockApplicationRepository.save(any()) } returns application

      val savedApplication = mockApplicationRepository.save(application)

      assertThat(savedApplication.previousReferralProbationRegion).isNotNull
      assertThat(savedApplication.previousReferralProbationRegion!!.id).isEqualTo(outOfRegionProbationRegion.id)
      assertThat(savedApplication.previousReferralProbationDeliveryUnit).isNotNull
      assertThat(savedApplication.previousReferralProbationDeliveryUnit!!.id).isEqualTo(outOfRegionPdu.id)
      assertThat(savedApplication.previousReferralProbationDeliveryUnit!!.name).isEqualTo("Out Of Region PDU")

      verify { mockApplicationRepository.save(application) }
    }

    @Test
    fun `should persist and retrieve null out of region fields when not set`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withOutOfRegionProbationRegion(null)
        .withoutOfRegionProbationDeliveryUnit(null)
        .produce()

      every { mockApplicationRepository.save(any()) } returns application

      val savedApplication = mockApplicationRepository.save(application)

      assertThat(savedApplication.previousReferralProbationRegion).isNull()
      assertThat(savedApplication.previousReferralProbationDeliveryUnit).isNull()

      assertThat(savedApplication.probationRegion).isNotNull
      assertThat(savedApplication.probationRegion.id).isEqualTo(user.probationRegion.id)

      verify { mockApplicationRepository.save(application) }
    }
  }
}
