package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssignmentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationSummaryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateNonAssignable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas2ApplicationServiceTest {

  @MockK(relaxed = true)
  lateinit var mockApplicationRepository: Cas2ApplicationRepository

  @MockK
  lateinit var mockLockableApplicationRepository: Cas2LockableApplicationRepository

  @MockK(relaxed = true)
  lateinit var mockApplicationSummaryRepository: ApplicationSummaryRepository

  @MockK
  lateinit var mockOffenderService: Cas2OffenderService

  @MockK
  lateinit var mockUserAccessService: Cas2UserAccessService

  @MockK
  lateinit var mockDomainEventService: Cas2DomainEventService

  @MockK
  lateinit var mockEmailNotificationService: EmailNotificationService

  @MockK
  lateinit var mockAssessmentService: Cas2AssessmentService

  @MockK
  lateinit var mockObjectMapper: ObjectMapper

  @MockK
  lateinit var mockNotifyConfig: NotifyConfig

  lateinit var applicationService: Cas2ApplicationService

  @BeforeEach
  fun setup() {
    applicationService = Cas2ApplicationService(
      mockApplicationRepository,
      mockLockableApplicationRepository,
      mockApplicationSummaryRepository,
      mockOffenderService,
      mockUserAccessService,
      mockDomainEventService,
      mockEmailNotificationService,
      mockAssessmentService,
      mockNotifyConfig,
      mockObjectMapper,
      "http://frontend/applications/#id",
      "http://frontend/assess/applications/#applicationId/overview",
    )
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class FindApplicationToAssign {
    private val user = Cas2UserEntityFactory().produce()
    private val nomsNumber = "ABC123"

    @Test
    fun `finds assignable application as the latest application's most recent status-update makes it is assignable`() {
      val application = createApplicationWithStatusUpdateEntity(
        statusUpdateEntityLabel = "Assignable",
      )
      every {
        mockApplicationRepository.findFirstByNomsNumberAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(application.nomsNumber!!)
      } returns application
      assertThat(applicationService.findApplicationToAssign(nomsNumber)).isEqualTo(application)
    }

    @ParameterizedTest
    @EnumSource
    fun `does not find assignable application as the latest application's most recent status-update is not assignable`(
      statusUpdateNonAssignable: Cas2StatusUpdateNonAssignable,
    ) {
      val application = createApplicationWithStatusUpdateEntity(
        statusUpdateEntityLabel = statusUpdateNonAssignable.label,
      )
      every {
        mockApplicationRepository.findFirstByNomsNumberAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(application.nomsNumber!!)
      } returns application
      assertThat(applicationService.findApplicationToAssign(nomsNumber)).isNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `finds assignable application as the latest application's status-update list is empty or null`(statusUpdatesListNull: Boolean) {
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withNomsNumber(nomsNumber)
        .withStatusUpdates(mutableListOf())
        .produce()

      if (statusUpdatesListNull) {
        application.statusUpdates = null
      }

      every {
        mockApplicationRepository.findFirstByNomsNumberAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(application.nomsNumber!!)
      } returns application
      assertThat(applicationService.findApplicationToAssign(nomsNumber)).isEqualTo(application)
    }

    private fun createApplicationWithStatusUpdateEntity(statusUpdateEntityLabel: String): Cas2ApplicationEntity {
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withNomsNumber(nomsNumber)
        .produce()

      val nonAssignableStatusUpdateEntity = Cas2StatusUpdateEntityFactory()
        .withApplication(application)
        .withLabel(statusUpdateEntityLabel)
        .withCreatedAt(OffsetDateTime.now())
        .produce()

      application.statusUpdates!!.clear()
      application.statusUpdates!!.add(nonAssignableStatusUpdateEntity)

      return application
    }
  }

  @Nested
  inner class GetAllSubmittedApplicationsForAssessor {
    @Test
    fun `returns Success result with entity from db`() {
      val applicationSummary = Cas2ApplicationSummaryEntityFactory.produce()

      PaginationConfig(defaultPageSize = 10).postInit()
      val page = mockk<Page<Cas2ApplicationSummaryEntity>>()
      val pageRequest = mockk<PageRequest>()
      val pageCriteria = PageCriteria(sortBy = "submitted_at", sortDirection = SortDirection.asc, page = 3)

      mockkStatic(PageRequest::class)

      every { PageRequest.of(2, 10, Sort.by("submitted_at").ascending()) } returns pageRequest
      every { page.content } returns listOf(applicationSummary)
      every { page.totalPages } returns 10
      every { page.totalElements } returns 100

      every {
        mockApplicationSummaryRepository.findBySubmittedAtIsNotNull(
          PageRequest.of(
            2,
            10,
            Sort.by(Sort.Direction.ASC, "submitted_at"),
          ),
        )
      } returns page

      val (applicationSummaries, metadata) = applicationService.getAllSubmittedApplicationsForAssessor(pageCriteria)

      assertThat(applicationSummaries).isEqualTo(listOf(applicationSummary))
      assertThat(metadata?.currentPage).isEqualTo(3)
      assertThat(metadata?.pageSize).isEqualTo(10)
      assertThat(metadata?.totalPages).isEqualTo(10)
      assertThat(metadata?.totalResults).isEqualTo(100)
    }
  }

  @Nested
  inner class ApplicationSummaries {

    private val prisonCode = "PRI"
    private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId(prisonCode).produce()
    private val pageCriteria = PageCriteria("createdAt", SortDirection.desc, 1)

    @BeforeEach
    fun setup() {
      PaginationConfig(defaultPageSize = 10).postInit()
    }

    @Test
    fun getAllocatedApplicationSummaries() {
      applicationService.getApplicationSummaries(
        user = user,
        pageCriteria = pageCriteria,
        assignmentType = AssignmentType.ALLOCATED,
      )

      verify {
        mockApplicationSummaryRepository.findApplicationsAssignedToUser(
          user.id,
          any(),
        )
      }
    }

    @Test
    fun getUnallocatedApplicationSummaries() {
      applicationService.getApplicationSummaries(
        user = user,
        pageCriteria = pageCriteria,
        assignmentType = AssignmentType.UNALLOCATED,
      )

      verify {
        mockApplicationSummaryRepository.findUnallocatedApplicationsInSamePrisonAsUser(
          prisonCode,
          any(),
        )
      }
    }

    @Test
    fun getDeallocatedApplicationSummaries() {
      applicationService.getApplicationSummaries(
        user = user,
        pageCriteria = pageCriteria,
        assignmentType = AssignmentType.DEALLOCATED,
      )

      verifySequence {
        mockApplicationRepository.findPreviouslyAssignedApplicationsInDifferentPrisonToUser(
          user.id,
          prisonCode,
        )
        mockApplicationSummaryRepository.findAllByIdIn(any(), any())
      }
    }

    @Test
    fun getInProgressApplicationSummaries() {
      applicationService.getApplicationSummaries(
        user = user,
        pageCriteria = pageCriteria,
        assignmentType = AssignmentType.IN_PROGRESS,
      )

      verify {
        mockApplicationSummaryRepository.findInProgressApplications(
          user.id.toString(),
          any(),
        )
      }
    }

    @Test
    fun getInSamePrisonApplicationSummaries() {
      applicationService.getApplicationSummaries(
        user = user,
        pageCriteria = pageCriteria,
        assignmentType = AssignmentType.PRISON,
      )

      verify {
        mockApplicationSummaryRepository.findAllocatedApplicationsInSamePrisonAsUser(
          prisonCode,
          any(),

        )
      }
    }
  }

  @Nested
  inner class GetApplicationForUser {
    @Test
    fun `where application does not exist returns NotFound result`() {
      val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThatCasResult(applicationService.getApplicationForUser(applicationId, user)).isNotFound(
        "Application",
        applicationId,
      )
    }

    @Test
    fun `where application is abandoned returns NotFound result`() {
      val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(any()) } returns
        Cas2ApplicationEntityFactory()
          .withCreatedByUser(
            Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
              .produce(),
          )
          .withAbandonedAt(OffsetDateTime.now())
          .produce()

      assertThatCasResult(applicationService.getApplicationForUser(applicationId, user)).isNotFound(
        "Application",
        applicationId,
      )
    }

    @Test
    fun `where user cannot access the application returns Unauthorised result`() {
      val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
        .produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(any()) } returns
        Cas2ApplicationEntityFactory()
          .withCreatedByUser(
            Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
              .produce(),
          )
          .produce()

      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns false

      assertThatCasResult(applicationService.getApplicationForUser(applicationId, user)).isUnauthorised()
    }

    @Test
    fun `where user can access the application returns Success result with entity from db`() {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
        .withId(userId)
        .withUsername(distinguishedName)
        .produce()

      val applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

      val result = applicationService.getApplicationForUser(applicationId, userEntity)

      assertThatCasResult(result).isSuccess().hasValueEqualTo(applicationEntity)
    }
  }

  @Nested
  inner class CreateApplication {
    @Test
    fun `returns Success with created Application`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"
      val personInfoResult = mockk<PersonInfoResult.Success.Full>()

      every { personInfoResult.crn } returns crn
      every { personInfoResult.offenderDetailSummary.otherIds.nomsNumber } returns "NOMS123"

      val user = userWithUsername(username)

      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0] as
          Cas2ApplicationEntity
      }

      val result = applicationService.createApplication(personInfoResult, user)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.crn).isEqualTo(crn)
        assertThat(it.createdByUser).isEqualTo(user)
      }
    }
  }

  @Nested
  inner class UpdateApplication {
    val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()

    @Test
    fun `returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThatCasResult(
        applicationService.updateApplication(
          applicationId = applicationId,
          data = "{}",
          user = user,
        ),
      ).isNotFound("Application", applicationId)
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
            .produce()
        }
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application

      assertThatCasResult(
        applicationService.updateApplication(
          applicationId = applicationId,
          data = "{}",
          user = user,
        ),
      ).isUnauthorised()
    }

    @Test
    fun `returns GeneralValidationError when application has already been submitted`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThatCasResult(result).isGeneralValidationError("This application has already been submitted")
    }

    @Test
    fun `returns GeneralValidationError when application has been abandoned`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withAbandonedAt(OffsetDateTime.now())
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThatCasResult(result).isGeneralValidationError("This application has been abandoned")
    }

    @ParameterizedTest
    @ValueSource(strings = ["<", "＜", "〈", "〈", ">", "＞", "〉", "〉", "<＜〈〈>＞〉〉"])
    fun `returns Success when an application, that contains removed malicious characters, is updated`(str: String) {
      val applicationId = UUID.fromString("dced02b1-8e3b-4ea5-bf99-1fba0ca1b87c")

      val updatedData = """
      {
        "aProperty": "val${str}ue"
      }
    """

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2ApplicationEntity
      }

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = updatedData,
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.data).isEqualTo(
          """
      {
        "aProperty": "value"
      }
    """,
        )
      }
    }

    @Test
    fun `returns Success with updated Application`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val updatedData = """
      {
        "aProperty": "value"
      }
    """

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2ApplicationEntity
      }

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = updatedData,
        user = user,
      )
      assertThatCasResult(result).isSuccess().with {
        assertThat(it.data).isEqualTo(updatedData)
      }
    }

    @Nested
    inner class AbandonApplication {
      val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()

      @Test
      fun `returns NotFound when application doesn't exist`() {
        val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

        assertThatCasResult(
          applicationService.abandonApplication(
            applicationId = applicationId,
            user = user,
          ),
        ).isNotFound("Application", applicationId)
      }

      @Test
      fun `returns Unauthorised when application doesn't belong to request user`() {
        val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withYieldedCreatedByUser {
            Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
              .produce()
          }
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
          application

        assertThatCasResult(
          applicationService.abandonApplication(
            applicationId = applicationId,
            user = user,
          ),
        ).isUnauthorised()
      }

      @Test
      fun `returns Conflict Error when application has already been submitted`() {
        val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(OffsetDateTime.now())
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
          application

        val result = applicationService.abandonApplication(
          applicationId = applicationId,
          user = user,
        )

        assertThatCasResult(result).isConflictError().hasMessage("This application has already been submitted")
      }

      @Test
      fun `returns Success when application has already been abandoned`() {
        val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withAbandonedAt(OffsetDateTime.now())
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
          application

        val result = applicationService.abandonApplication(
          applicationId = applicationId,
          user = user,
        )
        assertThatCasResult(result).isSuccess()
      }

      @Test
      fun `returns Success and deletes the application data`() {
        val applicationId = UUID.fromString("dced02b1-8e3b-4ea5-bf99-1fba0ca1b87c")

        val data = """
            {
              "aProperty": "value"
            }
      """

        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withData(data)
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
          application

        every { mockApplicationRepository.save(any()) } answers {
          it.invocation.args[0] as Cas2ApplicationEntity
        }

        val result = applicationService.abandonApplication(
          applicationId = applicationId,
          user = user,
        )

        assertThatCasResult(result).isSuccess().with { assertThat(it.data.isNullOrEmpty()) }
      }
    }

    @Nested
    inner class SubmitApplication {
      val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"
      val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
        .withUsername(this.username)
        .produce()
      val hdcEligibilityDate = LocalDate.parse("2023-03-30")
      val conditionalReleaseDate = LocalDate.parse("2023-04-29")

      private val submitCas2Application = SubmitCas2Application(
        translatedDocument = {},
        applicationId = applicationId,
        preferredAreas = "Leeds | Bradford",
        hdcEligibilityDate = hdcEligibilityDate,
        conditionalReleaseDate = conditionalReleaseDate,
        telephoneNumber = "123",
      )

      @BeforeEach
      fun setup() {
        every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns Cas2LockableApplicationEntity(
          UUID.randomUUID(),
        )
        every { mockObjectMapper.writeValueAsString(submitCas2Application.translatedDocument) } returns "{}"
        every { mockDomainEventService.saveCas2ApplicationSubmittedDomainEvent(any()) } just Runs
      }

      @Test
      fun `returns NotFound when application doesn't exist`() {
        val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

        assertThatCasResult(
          applicationService.submitApplication(
            submitCas2Application,
            user,
          ),
        ).isNotFound("Application", applicationId)

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `returns Unauthorised when application doesn't belong to request user`() {
        val differentUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .produce()

        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(differentUser)
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

        assertThatCasResult(applicationService.submitApplication(submitCas2Application, user)).isUnauthorised()

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `returns GeneralValidationError when application has already been submitted`() {
        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(OffsetDateTime.now())
          .produce()

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isGeneralValidationError("This application has already been submitted")

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `returns GeneralValidationError when application has already been abandoned`() {
        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withAbandonedAt(OffsetDateTime.now())
          .produce()

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isGeneralValidationError("This application has already been abandoned")

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `throws a validation error if InmateDetails (for prison code) are not available`() {
        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()

        every {
          mockApplicationRepository.findByIdOrNull(any())
        } returns application

        every { mockApplicationRepository.save(any()) } answers {
          it.invocation.args[0]
            as Cas2ApplicationEntity
        }

        // this call to the Prison API to find the referringPrisonCode when saving
        // the application.submitted domain event *should* never 404 or otherwise fail,
        // as when creating  the application initially a similar call was made.
        // If there is a problem with accessing the Prison API, we fail hard and
        // abort our attempt to submit the application.
        every {
          mockOffenderService.getInmateDetailByNomsNumber(any(), any())
        } returns AuthorisableActionResult.NotFound()

        assertGeneralValidationError("Inmate Detail not found")

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `throws an UpstreamApiException if prison code is null`() {
        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()

        every {
          mockApplicationRepository.findByIdOrNull(any())
        } returns application

        every { mockApplicationRepository.save(any()) } answers {
          it.invocation.args[0]
            as Cas2ApplicationEntity
        }

        // this call to the Prison API to find the referringPrisonCode when saving
        // the application.submitted domain event *should* always have a prison code,
        // but we need to account for possibility it may be missing.
        // If there is a problem with accessing the Prison API, we fail hard and
        // abort our attempt to submit the application and return a validation message.
        every {
          mockOffenderService.getInmateDetailByNomsNumber(any(), any())
        } returns AuthorisableActionResult.Success(InmateDetailFactory().produce())

        assertGeneralValidationError("No prison code available")

        assertEmailAndAssessmentsWereNotCreated()
      }

      private fun assertGeneralValidationError(message: String) {
        val result = applicationService.submitApplication(submitCas2Application, user)
        assertThatCasResult(result).isGeneralValidationError(message)
      }

      private fun assertEmailAndAssessmentsWereNotCreated() {
        verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
        verify(exactly = 0) { mockAssessmentService.createCas2Assessment(any()) }
      }

      @SuppressWarnings("CyclomaticComplexMethod")
      @Test
      fun `returns Success and stores event`() {
        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()

        val assessment = Cas2AssessmentEntityFactory().withApplication(application).produce()

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application

        val inmateDetail = InmateDetailFactory()
          .withAssignedLivingUnit(
            AssignedLivingUnit(
              agencyId = "BRI",
              locationId = 1234,
              description = "description",
              agencyName = "HMP Bristol",
            ),
          )
          .produce()

        every {
          mockOffenderService.getInmateDetailByNomsNumber(
            application.crn,
            application.nomsNumber.toString(),
          )
        } returns AuthorisableActionResult.Success(inmateDetail)

        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns "exampleAssessorInbox@example.com"
        every { mockNotifyConfig.emailAddresses.cas2ReplyToId } returns "def456"
        every { mockEmailNotificationService.sendEmail(any(), any(), any(), any()) } just Runs

        every { mockApplicationRepository.save(any()) } answers {
          it.invocation.args[0]
            as Cas2ApplicationEntity
        }

        every { mockAssessmentService.createCas2Assessment(any()) } returns assessment

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isSuccess().with { entity ->
          assertThat(entity.crn).isEqualTo(application.crn)
          assertThat(entity.preferredAreas).isEqualTo("Leeds | Bradford")
          assertThat(entity.hdcEligibilityDate).isEqualTo(hdcEligibilityDate)
          assertThat(entity.conditionalReleaseDate).isEqualTo(conditionalReleaseDate)
          verify(exactly = 1) { mockAssessmentService.createCas2Assessment(match { it.id == applicationId }) }
        }

        verify { mockApplicationRepository.save(any()) }

        verify(exactly = 1) {
          mockDomainEventService.saveCas2ApplicationSubmittedDomainEvent(
            match {
              val data = it.data.eventDetails

              it.applicationId == application.id &&
                data.personReference.noms == application.nomsNumber &&
                data.personReference.crn == application.crn &&
                data.applicationUrl == "http://frontend/applications/${application.id}" &&
                data.submittedBy.staffMember.username == username &&
                data.referringPrisonCode == "BRI" &&
                data.preferredAreas == "Leeds | Bradford" &&
                data.hdcEligibilityDate == hdcEligibilityDate &&
                data.conditionalReleaseDate == conditionalReleaseDate
            },
          )
        }

        verify(exactly = 1) {
          mockEmailNotificationService.sendEmail(
            "exampleAssessorInbox@example.com",
            Cas2NotifyTemplates.CAS2_APPLICATION_SUBMITTED,
            match {
              it["name"] == user.name &&
                it["email"] == user.email &&
                it["prisonNumber"] == application.nomsNumber &&
                it["telephoneNumber"] == application.telephoneNumber &&
                it["applicationUrl"] == "http://frontend/assess/applications/$applicationId/overview"
            },
            "def456",
          )
        }
      }
    }
  }

  private fun userWithUsername(username: String) = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
    .withUsername(username)
    .produce()
}
