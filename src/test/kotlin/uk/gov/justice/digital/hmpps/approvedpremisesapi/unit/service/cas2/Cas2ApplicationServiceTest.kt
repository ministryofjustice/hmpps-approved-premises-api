package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationServiceTest {
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockLockableApplicationRepository = mockk<Cas2LockableApplicationRepository>()
  private val mockApplicationSummaryRepository = mockk<ApplicationSummaryRepository>()
  private val mockJsonSchemaService = mockk<Cas2JsonSchemaService>()
  private val mockOffenderService = mockk<Cas2OffenderService>()
  private val mockUserAccessService = mockk<Cas2UserAccessService>()
  private val mockDomainEventService = mockk<Cas2DomainEventService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val mockAssessmentService = mockk<Cas2AssessmentService>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockNotifyConfig = mockk<NotifyConfig>()

  private val applicationService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationService(
    mockApplicationRepository,
    mockLockableApplicationRepository,
    mockApplicationSummaryRepository,
    mockJsonSchemaService,
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

  @Nested
  inner class GetAllSubmittedApplicationsForAssessor {
    @Test
    fun `returns Success result with entity from db`() {
      val applicationSummary = Cas2ApplicationSummaryEntity(
        id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
        crn = randomStringMultiCaseWithNumbers(6),
        nomsNumber = randomStringMultiCaseWithNumbers(6),
        userId = "836a9460-b177-433a-a0d9-262509092c9f",
        userName = "first last",
        createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
        submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
        hdcEligibilityDate = LocalDate.parse("2023-04-29"),
        latestStatusUpdateLabel = null,
        latestStatusUpdateStatusId = null,
        prisonCode = "BRI",
      )

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
  inner class GetApplicationsWithPrisonCode {
    val applicationSummary = Cas2ApplicationSummaryEntity(
      id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
      crn = randomStringMultiCaseWithNumbers(6),
      nomsNumber = randomStringMultiCaseWithNumbers(6),
      userId = "836a9460-b177-433a-a0d9-262509092c9f",
      userName = "first last",
      createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
      submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
      hdcEligibilityDate = LocalDate.parse("2023-04-29"),
      latestStatusUpdateLabel = null,
      latestStatusUpdateStatusId = null,
      prisonCode = "BRI",
    )
    val page = mockk<Page<Cas2ApplicationSummaryEntity>>()
    val pageCriteria = PageCriteria(sortBy = "submitted_at", sortDirection = SortDirection.asc, page = 3)
    val user = NomisUserEntityFactory().produce()
    val prisonCode = "BRI"

    fun testPrisonCodeWithIsSubmitted(isSubmitted: Boolean?) {
      every { page.content } returns listOf(applicationSummary)
      every { page.totalPages } returns 10
      every { page.totalElements } returns 100

      val (applicationSummaries, _) = applicationService.getApplications(prisonCode, isSubmitted, user, pageCriteria)

      assertThat(applicationSummaries).isEqualTo(listOf(applicationSummary))
    }

    @Test
    fun `return all applications when prisonCode is specified and isSubmitted is null`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockApplicationSummaryRepository.findByPrisonCode(
          prisonCode,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testPrisonCodeWithIsSubmitted(null)
    }

    @Test
    fun `return submitted prison applications when prisonCode is specified and isSubmitted is true`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockApplicationSummaryRepository.findByPrisonCodeAndSubmittedAtIsNotNull(
          prisonCode,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testPrisonCodeWithIsSubmitted(true)
    }

    @Test
    fun `return unsubmitted prison applications when prisonCode is specified and isSubmitted is false`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockApplicationSummaryRepository.findByPrisonCodeAndSubmittedAtIsNull(
          prisonCode,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testPrisonCodeWithIsSubmitted(false)
    }
  }

  @Nested
  inner class GetApplicationsWithoutPrisonCode {
    val applicationSummary = Cas2ApplicationSummaryEntity(
      id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
      crn = randomStringMultiCaseWithNumbers(6),
      nomsNumber = randomStringMultiCaseWithNumbers(6),
      userId = "836a9460-b177-433a-a0d9-262509092c9f",
      userName = "first last",
      createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
      submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
      hdcEligibilityDate = LocalDate.parse("2023-04-29"),
      latestStatusUpdateLabel = null,
      latestStatusUpdateStatusId = null,
      prisonCode = "BRI",
    )
    val page = mockk<Page<Cas2ApplicationSummaryEntity>>()
    val pageCriteria = PageCriteria(sortBy = "createdAt", sortDirection = SortDirection.asc, page = 3)
    val user = NomisUserEntityFactory().withId(UUID.fromString(applicationSummary.userId)).produce()

    fun testNullPrisonCodeWithIsSubmitted(isSubmitted: Boolean?) {
      every { page.content } returns listOf(applicationSummary)
      every { page.totalPages } returns 10
      every { page.totalElements } returns 100

      val (applicationSummaries, _) = applicationService.getApplications(null, isSubmitted, user, pageCriteria)

      assertThat(applicationSummaries).isEqualTo(listOf(applicationSummary))
    }

    @Test
    fun `return submitted prison applications when prisonCode is null and isSubmitted is null`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockApplicationSummaryRepository.findByUserId(
          applicationSummary.userId,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testNullPrisonCodeWithIsSubmitted(null)
    }

    @Test
    fun `return submitted prison applications when prisonCode is null and isSubmitted is true`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockApplicationSummaryRepository.findByUserIdAndSubmittedAtIsNotNull(
          applicationSummary.userId,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testNullPrisonCodeWithIsSubmitted(true)
    }

    @Test
    fun `return unsubmitted prison applications when prisonCode is null and isSubmitted is false`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockApplicationSummaryRepository.findByUserIdAndSubmittedAtIsNull(
          applicationSummary.userId,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testNullPrisonCodeWithIsSubmitted(false)
    }
  }

  @Nested
  inner class GetApplicationForUser {
    @Test
    fun `where application does not exist returns NotFound result`() {
      val user = NomisUserEntityFactory().produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThatCasResult(applicationService.getApplicationForUser(applicationId, user)).isNotFound(
        "Application",
        applicationId,
      )
    }

    @Test
    fun `where application is abandoned returns NotFound result`() {
      val user = NomisUserEntityFactory().produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(any()) } returns
        Cas2ApplicationEntityFactory()
          .withCreatedByUser(
            NomisUserEntityFactory()
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
      val user = NomisUserEntityFactory()
        .produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(any()) } returns
        Cas2ApplicationEntityFactory()
          .withCreatedByUser(
            NomisUserEntityFactory()
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

      val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
        .withSchema("{}")
        .produce()

      val userEntity = NomisUserEntityFactory()
        .withId(userId)
        .withNomisUsername(distinguishedName)
        .produce()

      val applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce()

      every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers {
        it.invocation
          .args[0] as Cas2ApplicationEntity
      }
      every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

      val result = applicationService.getApplicationForUser(applicationId, userEntity)

      assertThatCasResult(result).isSuccess().hasValueEqualTo(applicationEntity)
    }
  }

  @Nested
  inner class CreateApplication {
    @Test
    fun `returns FieldValidationError when Offender is not found`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      every { mockOffenderService.getCaseDetail(crn) } returns ClientResult.Failure.StatusCode(
        method = HttpMethod.GET,
        path = "/probation-cases/$crn/details",
        status = HttpStatus.NOT_FOUND,
        body = null,
        isPreemptivelyCachedResponse = false,
      )

      val user = userWithUsername(username)

      val result = applicationService.createApplication(crn, user, "jwt")

      assertThatCasResult(result).isNotFound("CaseDetail", crn)
    }

    @Test
    fun `returns Unauthorised when user is not authorised to view CRN`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      every { mockOffenderService.getCaseDetail(crn) } returns ClientResult.Failure.StatusCode(
        method = HttpMethod.GET,
        path = "/probation-cases/$crn/details",
        status = HttpStatus.FORBIDDEN,
        body = null,
        isPreemptivelyCachedResponse = false,
      )

      val user = userWithUsername(username)

      val result = applicationService.createApplication(crn, user, "jwt")

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `returns Success with created Application`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      val schema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val user = userWithUsername(username)

      every { mockOffenderService.getCaseDetail(crn) } returns ClientResult.Success(HttpStatus.OK, CaseDetailFactory().produce())

      every { mockJsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java) } returns schema
      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0] as
          Cas2ApplicationEntity
      }

      val result = applicationService.createApplication(crn, user, "jwt")

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.crn).isEqualTo(crn)
        assertThat(it.createdByUser).isEqualTo(user)
      }
    }
  }

  @Nested
  inner class UpdateApplication {
    val user = NomisUserEntityFactory().produce()

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
          NomisUserEntityFactory()
            .produce()
        }
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
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

      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
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

      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withAbandonedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThatCasResult(result).isGeneralValidationError("This application has been abandoned")
    }

    @Test
    fun `returns GeneralValidationError when application schema is outdated`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThatCasResult(result).isGeneralValidationError("The schema version is outdated")
    }

    @ParameterizedTest
    @ValueSource(strings = ["<", "＜", "〈", "〈", ">", "＞", "〉", "〉", "<＜〈〈>＞〉〉"])
    fun `returns Success when an application, that contains removed malicious characters, is updated`(str: String) {
      val applicationId = UUID.fromString("dced02b1-8e3b-4ea5-bf99-1fba0ca1b87c")

      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()
      val updatedData = """
      {
        "aProperty": "val${str}ue"
      }
    """

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application
      every {
        mockJsonSchemaService.getNewestSchema(
          Cas2ApplicationJsonSchemaEntity::class
            .java,
        )
      } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
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

      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()
      val updatedData = """
      {
        "aProperty": "value"
      }
    """

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java) } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
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
      val user = NomisUserEntityFactory().produce()

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
            NomisUserEntityFactory()
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

        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(OffsetDateTime.now())
          .produce()
          .apply {
            schemaUpToDate = true
          }

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

        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withAbandonedAt(OffsetDateTime.now())
          .produce()
          .apply {
            schemaUpToDate = true
          }

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

        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()
        val data = """
            {
              "aProperty": "value"
            }
      """

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withData(data)
          .produce()
          .apply {
            schemaUpToDate = true
          }

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
      val user = NomisUserEntityFactory()
        .withNomisUsername(this.username)
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
        val differentUser = NomisUserEntityFactory()
          .produce()

        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(differentUser)
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
          application

        assertThatCasResult(applicationService.submitApplication(submitCas2Application, user)).isUnauthorised()

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `returns GeneralValidationError when application schema is outdated`() {
        val application = Cas2ApplicationEntityFactory()
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()
          .apply {
            schemaUpToDate = false
          }

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isGeneralValidationError("The schema version is outdated")

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `returns GeneralValidationError when application has already been submitted`() {
        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(OffsetDateTime.now())
          .produce()
          .apply {
            schemaUpToDate = true
          }

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isGeneralValidationError("This application has already been submitted")

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `returns GeneralValidationError when application has already been abandoned`() {
        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withAbandonedAt(OffsetDateTime.now())
          .produce()

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isGeneralValidationError("This application has already been abandoned")

        assertEmailAndAssessmentsWereNotCreated()
      }

      @Test
      fun `throws a validation error if InmateDetails (for prison code) are not available`() {
        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()
          .apply {
            schemaUpToDate = true
          }

        every {
          mockApplicationRepository.findByIdOrNull(any())
        } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(any()) } returns
          application
        every { mockJsonSchemaService.validate(any(), any()) } returns true

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
        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()
          .apply {
            schemaUpToDate = true
          }

        every {
          mockApplicationRepository.findByIdOrNull(any())
        } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(any()) } returns
          application
        every { mockJsonSchemaService.validate(any(), any()) } returns true

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
        val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestSchema)
          .withId(applicationId)
          .withCreatedByUser(user)
          .withSubmittedAt(null)
          .produce()
          .apply {
            schemaUpToDate = true
          }

        every {
          mockApplicationRepository.findByIdOrNull(applicationId)
        } returns application
        every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
          application
        every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true

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

        every { mockNotifyConfig.templates.cas2ApplicationSubmitted } returns "abc123"
        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns "exampleAssessorInbox@example.com"
        every { mockNotifyConfig.emailAddresses.cas2ReplyToId } returns "def456"
        every { mockEmailNotificationService.sendEmail(any(), any(), any(), any()) } just Runs

        every { mockApplicationRepository.save(any()) } answers {
          it.invocation.args[0]
            as Cas2ApplicationEntity
        }

        every { mockAssessmentService.createCas2Assessment(any()) } returns any()

        val result = applicationService.submitApplication(submitCas2Application, user)

        assertThatCasResult(result).isSuccess().with {
          assertThat(it.crn).isEqualTo(application.crn)
          assertThat(it.preferredAreas).isEqualTo("Leeds | Bradford")
          assertThat(it.hdcEligibilityDate).isEqualTo(hdcEligibilityDate)
          assertThat(it.conditionalReleaseDate).isEqualTo(conditionalReleaseDate)
          verify(exactly = 1) { mockAssessmentService.createCas2Assessment(it) }
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
            "abc123",
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

  private fun userWithUsername(username: String) = NomisUserEntityFactory()
    .withNomisUsername(username)
    .produce()
}
