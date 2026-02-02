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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.listOf

class Cas3ApplicationServiceTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockTemporaryAccommodationApplicationRepository = mockk<TemporaryAccommodationApplicationRepository>()
  private val mockLockableApplicationRepository = mockk<LockableApplicationRepository>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockUserRepository = mockk<UserRepository>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockUserService = mockk<UserService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockOffenderRisksService = mockk<OffenderRisksService>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockProbationRegionRepository = mockk<ProbationRegionRepository>()
  private val mockCas3v2BookingService = mockk<Cas3v2BookingService>()

  private val cas3ApplicationService = Cas3ApplicationService(
    mockApplicationRepository,
    mockTemporaryAccommodationApplicationRepository,
    mockLockableApplicationRepository,
    mockProbationDeliveryUnitRepository,
    mockProbationRegionRepository,
    mockUserRepository,
    mockUserService,
    mockUserAccessService,
    mockAssessmentService,
    mockCas3DomainEventService,
    mockOffenderService,
    mockOffenderRisksService,
    mockObjectMapper,
    mockCas3v2BookingService,
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
  inner class GetSuitableApplicationByCrn {
    val crn = "X123456"

    @Test
    fun `getSuitableApplicationByCrn where application does not exist returns null`() {
      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns emptyList()

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(
          crn,
        ),
      ).isNull()
    }

    @Test
    fun `getSuitableApplicationByCrn returns inProgress application when only inProgress exists`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(null)
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(application)
      every { mockCas3v2BookingService.getLatestBookingStatus(application.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          application.id,
          ApplicationStatus.inProgress,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns suitable application when one has been submitted`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(application)
      every { mockCas3v2BookingService.getLatestBookingStatus(application.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          application.id,
          ApplicationStatus.submitted,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns latest suitable application when multiple have been submitted`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application1 = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now().minusDays(1))
        .produce()

      val application2 = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now().minusDays(2))
        .produce()

      val application3 = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(
        application1,
        application3,
        application2,
      )
      every { mockCas3v2BookingService.getLatestBookingStatus(application3.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          application3.id,
          ApplicationStatus.submitted,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns suitable application when one has been submitted requesting further info`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      assessment.clarificationNotes = mutableListOf(
        AssessmentClarificationNoteEntityFactory()
          .withAssessment(assessment)
          .withCreatedBy(user)
          .produce(),
      )

      application.assessments = mutableListOf(assessment)

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(application)
      every { mockCas3v2BookingService.getLatestBookingStatus(application.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          application.id,
          ApplicationStatus.requestedFurtherInformation,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns suitable application when one has been submitted requesting further info instead of one that has just been submitted`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val applicationRequestInfo = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now().minusDays(1))
        .produce()

      val applicationSubmitted1 = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val applicationSubmitted2 = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(applicationRequestInfo)
        .produce()

      assessment.clarificationNotes = mutableListOf(
        AssessmentClarificationNoteEntityFactory()
          .withAssessment(assessment)
          .withCreatedBy(user)
          .produce(),
      )

      applicationRequestInfo.assessments = mutableListOf(assessment)

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(
        applicationSubmitted1,
        applicationRequestInfo,
        applicationSubmitted2,
      )
      every { mockCas3v2BookingService.getLatestBookingStatus(applicationRequestInfo.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          applicationRequestInfo.id,
          ApplicationStatus.requestedFurtherInformation,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns submitted over inProgress regardless of date`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val inProgressApplication = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withCreatedAt(OffsetDateTime.now())
        .withSubmittedAt(null)
        .produce()

      val submittedApplication = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withCreatedAt(OffsetDateTime.now().minusDays(10))
        .withSubmittedAt(OffsetDateTime.now().minusDays(10))
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(
        inProgressApplication,
        submittedApplication,
      )
      every { mockCas3v2BookingService.getLatestBookingStatus(submittedApplication.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          submittedApplication.id,
          ApplicationStatus.submitted,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn uses createdAt as tiebreaker when submittedAt is null`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val olderInProgressApplication = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withCreatedAt(OffsetDateTime.now().minusDays(5))
        .withSubmittedAt(null)
        .produce()

      val newerInProgressApplication = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withCreatedAt(OffsetDateTime.now())
        .withSubmittedAt(null)
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(
        olderInProgressApplication,
        newerInProgressApplication,
      )
      every { mockCas3v2BookingService.getLatestBookingStatus(newerInProgressApplication.id) } returns null

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          newerInProgressApplication.id,
          ApplicationStatus.inProgress,
          bookingStatus = null,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns booking status when booking exists`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(application)
      every { mockCas3v2BookingService.getLatestBookingStatus(application.id) } returns Cas3BookingStatus.confirmed

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          application.id,
          ApplicationStatus.submitted,
          bookingStatus = Cas3BookingStatus.confirmed,
        ),
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns latest booking status when multiple bookings exist`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByCrn(crn) } returns listOf(application)
      every { mockCas3v2BookingService.getLatestBookingStatus(application.id) } returns Cas3BookingStatus.arrived

      assertThat(
        cas3ApplicationService.getSuitableApplicationByCrn(crn),
      ).isEqualTo(
        Cas3SuitableApplication(
          application.id,
          ApplicationStatus.submitted,
          bookingStatus = Cas3BookingStatus.arrived,
        ),
      )
    }
  }

  @Nested
  inner class GetApplicationForUsername {
    @Test
    fun `getApplicationForUsername where application does not exist returns NotFound result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
      val deletedApplication = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          UserEntityFactory()
            .withProbationRegion(probationRegion)
            .produce()
        }
        .withProbationRegion(probationRegion)
        .withDeletedAt(OffsetDateTime.now().minusDays(10))
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByIdOrNull(applicationId) } returns deletedApplication

      assertThat(
        cas3ApplicationService.getApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.NotFound,
      ).isTrue
    }

    @Test
    fun `getApplicationForUsername where application was deleted returns NotFound result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockTemporaryAccommodationApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        cas3ApplicationService.getApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.NotFound,
      ).isTrue
    }

    @Test
    fun `getApplicationForUsername where user cannot access the application returns Unauthorised result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      every { mockUserRepository.findByDeliusUsername(any()) } returns UserEntityFactory()
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion { probationRegion }
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByIdOrNull(any()) } returns TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory()
            .withYieldedProbationRegion { probationRegion }
            .produce(),
        )
        .withProbationRegion(probationRegion)
        .produce()

      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns false

      assertThat(
        cas3ApplicationService.getApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `getApplicationForUsername where user can access the application returns Success result with entity from db`() {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion { probationRegion }
        .produce()

      val applicationEntity = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withProbationRegion(probationRegion)
        .produce()

      every { mockTemporaryAccommodationApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
      every { mockUserRepository.findByDeliusUsername(any()) } returns userEntity
      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

      val result = cas3ApplicationService.getApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      assertThat(result.value).isEqualTo(applicationEntity)
    }
  }

  @Nested
  inner class GetApplicationSummaries {
    @Test
    fun `Get all applications where Probation Officer exists returns applications returned from repository`() {
      val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
      val deliusUsername = "SOMEPERSON"
      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(deliusUsername)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val applicationSummaries = listOf(
        object : TemporaryAccommodationApplicationSummary {
          override fun getRiskRatings(): String? = null
          override fun getId(): UUID = UUID.fromString("8ecbbd9c-3c66-4f0b-8f21-87f537676422")
          override fun getCrn(): String = "CRN123"
          override fun getCreatedByUserId(): UUID = UUID.fromString("60d0a768-1d05-4538-a6fd-78eb723dd310")
          override fun getCreatedAt(): Instant = Instant.parse("2023-04-20T10:11:00+01:00")
          override fun getSubmittedAt(): Instant? = null
          override fun getLatestAssessmentSubmittedAt(): Instant? = null
          override fun getLatestAssessmentDecision(): AssessmentDecision? = null
          override fun getLatestAssessmentHasClarificationNotesWithoutResponse(): Boolean = false
          override fun getHasBooking(): Boolean = false
        },
      )

      every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity
      every { mockApplicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(userId) } returns applicationSummaries

      assertThat(
        cas3ApplicationService.getApplicationSummariesForUser(userEntity),
      ).containsAll(applicationSummaries)
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
  inner class CreateApplication {
    @Test
    fun `createApplication returns Unauthorised when user doesn't have CAS3_REFERRER role`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val inmateDetail = createInmateDetail(InmateStatus.IN, "Bristol Prison")
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )
      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_ASSESSOR)
            .produce(),
        )
      }

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `createApplication returns FieldValidationError when CRN does not exist`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val inmateDetail = createInmateDetail(InmateStatus.IN, "Bristol Prison")
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )
      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.NotFound()

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.crn", "doesNotExist")
    }

    @Test
    fun `createApplication returns FieldValidationError when CRN is LAO restricted`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val inmateDetail = createInmateDetail(InmateStatus.IN, "Bristol Prison")
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )
      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Unauthorised()

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.crn", "userPermission")
    }

    @Test
    fun `createApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val inmateDetail = createInmateDetail(InmateStatus.IN, "HMP Bristol")
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )

      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        null,
        null,
        null,
        personInfo = personInfo,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.convictionId", "empty")
        .hasMessage("$.deliusEventNumber", "empty")
        .hasMessage("$.offenceId", "empty")
    }

    @Test
    fun `createApplication returns Success with created Application + persisted Risk data`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val agencyName = "HMP Bristol"
      val inmateDetail = createInmateDetail(InmateStatus.IN, agencyName)
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val riskRatings = PersonRisksFactory()
        .withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
        .withMappa(
          RiskWithStatus(
            value = Mappa(
              level = "",
              lastUpdated = LocalDate.parse("2022-12-12"),
            ),
          ),
        )
        .withFlags(
          RiskWithStatus(
            value = listOf(
              "flag1",
              "flag2",
            ),
          ),
        )
        .produce()

      every { mockOffenderRisksService.getPersonRisks(crn) } returns riskRatings

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.riskRatings).isEqualTo(riskRatings)
        assertThat(it.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    @Test
    fun `createApplication returns Success with created Application with prison name when person status is TRN`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val agencyName = "HMP Bristol"
      val inmateDetail = createInmateDetail(InmateStatus.TRN, agencyName)
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val riskRatings = PersonRisksFactory()
        .withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
        .withMappa(
          RiskWithStatus(
            value = Mappa(
              level = "",
              lastUpdated = LocalDate.parse("2022-12-12"),
            ),
          ),
        )
        .withFlags(
          RiskWithStatus(
            value = listOf(
              "flag1",
              "flag2",
            ),
          ),
        )
        .produce()

      every { mockOffenderRisksService.getPersonRisks(crn) } returns riskRatings

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.riskRatings).isEqualTo(riskRatings)
        assertThat(it.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    @Test
    fun `createApplication returns Success with created Application without prison name when person status is Out`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val inmateDetail = createInmateDetail(InmateStatus.OUT, "HMP Bristol")
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val riskRatings = PersonRisksFactory()
        .withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
        .withMappa(
          RiskWithStatus(
            value = Mappa(
              level = "",
              lastUpdated = LocalDate.parse("2022-12-12"),
            ),
          ),
        )
        .withFlags(
          RiskWithStatus(
            value = listOf(
              "flag1",
              "flag2",
            ),
          ),
        )
        .produce()

      every { mockOffenderRisksService.getPersonRisks(crn) } returns riskRatings

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isSuccess().with {
        val temporaryAccommodationApplication = it
        assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
        assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isNull()
      }
    }

    @Test
    fun `createApplication returns Success with created Application without prison name when assignedLivingUnit is null`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val offenderDetailSummary = createOffenderDetailsSummary(crn)
      val inmateDetail = createInmateDetail(InmateStatus.IN, null)
      val personInfo = PersonInfoResult.Success.Full(
        crn = crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetail,
      )

      val user = userWithUsername(username).apply {
        this.roles.add(
          UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS3_REFERRER)
            .produce(),
        )
      }

      every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val riskRatings = PersonRisksFactory()
        .withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
        .withMappa(
          RiskWithStatus(
            value = Mappa(
              level = "",
              lastUpdated = LocalDate.parse("2022-12-12"),
            ),
          ),
        )
        .withFlags(
          RiskWithStatus(
            value = listOf(
              "flag1",
              "flag2",
            ),
          ),
        )
        .produce()

      every { mockOffenderRisksService.getPersonRisks(crn) } returns riskRatings

      val result = cas3ApplicationService.createApplication(
        crn,
        user,
        123,
        "1",
        "A12HI",
        personInfo = personInfo,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.riskRatings).isEqualTo(riskRatings)
        assertThat(it.prisonNameOnCreation).isNull()
      }
    }

    private fun userWithUsername(username: String) = UserEntityFactory()
      .withDeliusUsername(username)
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(ApAreaEntityFactory().produce())
          .produce(),
      )
      .produce()

    private fun createInmateDetail(
      status: InmateStatus,
      agencyName: String?,
    ) = InmateDetail(
      offenderNo = "NOMS321",
      assignedLivingUnit = agencyName?.let {
        AssignedLivingUnit(
          agencyId = "BRI",
          locationId = 5,
          description = "B-2F-004",
          agencyName = it,
        )
      },
      custodyStatus = status,
    )

    private fun createOffenderDetailsSummary(crn: String) = OffenderDetailSummary(
      offenderId = 547839,
      title = "Mr",
      firstName = "Greggory",
      middleNames = listOf(),
      surname = "Someone",
      previousSurname = null,
      preferredName = null,
      dateOfBirth = LocalDate.parse("1980-09-12"),
      gender = "Male",
      otherIds = OffenderIds(
        crn = crn,
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = "PNC456",
      ),
      offenderProfile = OffenderProfile(
        ethnicity = "White and Asian",
        nationality = "Spanish",
        secondaryNationality = null,
        notes = null,
        immigrationStatus = null,
        offenderLanguages = OffenderLanguages(
          primaryLanguage = null,
          otherLanguages = listOf(),
          languageConcerns = null,
          requiresInterpreter = null,
        ),
        religion = "Sikh",
        sexualOrientation = null,
        offenderDetails = null,
        remandStatus = null,
        riskColour = null,
        disabilities = listOf(),
        genderIdentity = null,
        selfDescribedGender = null,
      ),
      softDeleted = null,
      currentDisposal = "",
      partitionArea = null,
      currentRestriction = false,
      currentExclusion = false,
      isActiveProbationManagedSentence = false,
    )
  }

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
