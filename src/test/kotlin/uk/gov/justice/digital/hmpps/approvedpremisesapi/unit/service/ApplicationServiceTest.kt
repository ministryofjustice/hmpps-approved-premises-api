package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService.Cas1ApplicationUpdateFields
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApprovedPremisesApplicationAccessLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService as Cas3DomainEventService

class ApplicationServiceTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockOfflineApplicationRepository = mockk<OfflineApplicationRepository>()
  private val mockApplicationTimelineNoteService = mockk<ApplicationTimelineNoteService>()
  private val mockApplicationTimelineNoteTransformer = mockk<ApplicationTimelineNoteTransformer>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockApplicationTeamCodeRepository = mockk<ApplicationTeamCodeRepository>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val mockAssessmentClarificationNoteTransformer = mockk<AssessmentClarificationNoteTransformer>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockApAreaRepository = mockk<ApAreaRepository>()
  private val applicationTimelineTransformerMock = mockk<ApplicationTimelineTransformer>()
  private val mockDomainEventTransformer = mockk<DomainEventTransformer>()
  private val mockCas1ApplicationDomainEventService = mockk<Cas1ApplicationDomainEventService>()

  private val applicationService = ApplicationService(
    mockUserRepository,
    mockApplicationRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockUserService,
    mockAssessmentService,
    mockOfflineApplicationRepository,
    mockApplicationTimelineNoteService,
    mockApplicationTimelineNoteTransformer,
    mockDomainEventService,
    mockCas3DomainEventService,
    mockApDeliusContextApiClient,
    mockApplicationTeamCodeRepository,
    mockEmailNotificationService,
    mockUserAccessService,
    NotifyConfig(),
    mockAssessmentClarificationNoteTransformer,
    mockObjectMapper,
    "http://frontend/applications/#id",
    mockApAreaRepository,
    applicationTimelineTransformerMock,
    mockDomainEventTransformer,
    mockCas1ApplicationDomainEventService,
  )

  @Test
  fun `Get all applications where Probation Officer with provided distinguished name does not exist returns empty list`() {
    val distinguishedName = "SOMEPERSON"

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns null

    assertThat(
      applicationService.getAllApplicationsForUsername(
        distinguishedName,
        ServiceName.approvedPremises,
      ),
    ).isEmpty()
  }

  @Test
  fun `Get all applications where Probation Officer exists returns applications returned from repository`() {
    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    val applicationSummaries = listOf(
      object : ApprovedPremisesApplicationSummary {
        override fun getIsWomensApplication(): Boolean? = true
        override fun getIsPipeApplication(): Boolean? = true
        override fun getIsEsapApplication() = true
        override fun getIsEmergencyApplication() = true
        override fun getArrivalDate(): Timestamp? = null
        override fun getRiskRatings(): String? = null
        override fun getId(): UUID = UUID.fromString("8ecbbd9c-3c66-4f0b-8f21-87f537676422")
        override fun getCrn(): String = "CRN123"
        override fun getCreatedByUserId(): UUID = UUID.fromString("60d0a768-1d05-4538-a6fd-78eb723dd310")
        override fun getCreatedAt(): Timestamp = Timestamp.from(Instant.parse("2023-04-20T10:11:00+01:00"))
        override fun getSubmittedAt(): Timestamp? = null
        override fun getTier(): String? = null
        override fun getStatus(): String = ApprovedPremisesApplicationStatus.started.toString()
        override fun getIsWithdrawn(): Boolean = false
      },
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockUserAccessService.getApprovedPremisesApplicationAccessLevelForUser(userEntity) } returns ApprovedPremisesApplicationAccessLevel.TEAM
    every { mockApplicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(userId) } returns applicationSummaries
    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    applicationSummaries.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.getCrn()) } returns true
    }

    assertThat(
      applicationService.getAllApplicationsForUsername(
        distinguishedName,
        ServiceName.approvedPremises,
      ),
    ).containsAll(applicationSummaries)
  }

  @Test
  fun `getApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is AuthorisableActionResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where user cannot access the application returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockUserRepository.findByDeliusUsername(any()) } returns UserEntityFactory()
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockApplicationRepository.findByIdOrNull(any()) } returns ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .produce()

    every { mockUserAccessService.userCanViewApplication(any(), any()) } returns false

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is AuthorisableActionResult.Unauthorised,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where user can access the application returns Success result with entity from db`() {
    val distinguishedName = "SOMEPERSON"
    val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val applicationEntity = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(any()) } returns userEntity
    every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

    val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity).isEqualTo(applicationEntity)
  }

  @Test
  fun `createApprovedPremisesApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    val user = userWithUsername(username)

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAMCODE"),
      ),
    )

    val result = applicationService.createApprovedPremisesApplication(offenderDetails, user, "jwt", null, null, null)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.convictionId", "empty")
    assertThat(result.validationMessages).containsEntry("$.deliusEventNumber", "empty")
    assertThat(result.validationMessages).containsEntry("$.offenceId", "empty")
  }

  @Test
  fun `createApprovedPremisesApplication returns Success with created Application, persists Risk data and Offender name`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username)

    every { mockOffenderService.getOASysNeeds(crn) } returns AuthorisableActionResult.Success(
      NeedsDetailsFactory().produce(),
    )

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAMCODE"),
      ),
    )

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.saveAndFlush(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockApplicationTeamCodeRepository.save(any()) } answers { it.invocation.args[0] as ApplicationTeamCodeEntity }

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

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val result = applicationService.createApprovedPremisesApplication(offenderDetails, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.Success).isTrue
    result as ValidatableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)
    assertThat(result.entity.createdByUser).isEqualTo(user)
    val approvedPremisesApplication = result.entity as ApprovedPremisesApplicationEntity
    assertThat(approvedPremisesApplication.riskRatings).isEqualTo(riskRatings)
    assertThat(approvedPremisesApplication.name).isEqualTo("${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Unauthorised when user doesn't have CAS3_REFERRER role`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InOutStatus.IN, "Bristol Prison")
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

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Unauthorised<*>).isTrue
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN does not exist`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InOutStatus.IN, "Bristol Prison")
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

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.crn", "doesNotExist")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN is LAO restricted`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InOutStatus.IN, "Bristol Prison")
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

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.crn", "userPermission")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InOutStatus.IN, "HMP Bristol")
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

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      null,
      null,
      null,
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.convictionId", "empty")
    assertThat(validationResult.validationMessages).containsEntry("$.deliusEventNumber", "empty")
    assertThat(validationResult.validationMessages).containsEntry("$.offenceId", "empty")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application + persisted Risk data`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val agencyName = "HMP Bristol"
    val inmateDetail = createInmateDetail(InOutStatus.IN, agencyName)
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
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

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.Success).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.Success
    val temporaryAccommodationApplication = validationResult.entity as TemporaryAccommodationApplicationEntity
    assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
    assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isEqualTo(agencyName)
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application with prison name when person status is TRN`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val agencyName = "HMP Bristol"
    val inmateDetail = createInmateDetail(InOutStatus.TRN, agencyName)
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
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

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.Success).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.Success
    val temporaryAccommodationApplication = validationResult.entity as TemporaryAccommodationApplicationEntity
    assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
    assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isEqualTo(agencyName)
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application without prison name when person status is Out`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InOutStatus.OUT, "HMP Bristol")
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
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

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.Success).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.Success
    val temporaryAccommodationApplication = validationResult.entity as TemporaryAccommodationApplicationEntity
    assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
    assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isNull()
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application without prison name when assignedLivingUnit is null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InOutStatus.IN, null)
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
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

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val actionResult = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      "jwt",
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThat(actionResult is AuthorisableActionResult.Success).isTrue()
    actionResult as AuthorisableActionResult.Success
    assertThat(actionResult.entity is ValidatableActionResult.Success).isTrue
    val validationResult = actionResult.entity as ValidatableActionResult.Success
    val temporaryAccommodationApplication = validationResult.entity as TemporaryAccommodationApplicationEntity
    assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
    assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isNull()
  }

  @Nested
  inner class UpdateApplicationCas1 {

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

    val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()
    val updatedData = """
      {
        "aProperty": "value"
      }
    """

    val application = ApprovedPremisesApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    @Test
    fun `updateApprovedPremisesApplication returns NotFound when application doesn't exist`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        applicationService.updateApprovedPremisesApplication(
          applicationId = applicationId,
          Cas1ApplicationUpdateFields(
            isWomensApplication = false,
            isPipeApplication = null,
            isEmergencyApplication = false,
            isEsapApplication = false,
            releaseType = null,
            arrivalDate = null,
            data = "{}",
            isInapplicable = null,
          ),
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `updateApprovedPremisesApplication returns Unauthorised when application doesn't belong to request user`() {
      every { mockUserService.getUserForRequest() } returns UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      assertThat(
        applicationService.updateApprovedPremisesApplication(
          applicationId = applicationId,
          Cas1ApplicationUpdateFields(
            isWomensApplication = false,
            isPipeApplication = null,
            isEmergencyApplication = false,
            isEsapApplication = false,
            releaseType = null,
            arrivalDate = null,
            data = "{}",
            isInapplicable = null,
          ),
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `updateApprovedPremisesApplication returns GeneralValidationError when application schema is outdated`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      application.schemaUpToDate = false

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = false,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
        ),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `updateApprovedPremisesApplication returns GeneralValidationError when application has already been submitted`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      application.submittedAt = OffsetDateTime.now()

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = false,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
        ),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `updateApprovedPremisesApplication returns Success with updated Application`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = true,
          isEmergencyApplication = false,
          isEsapApplication = false,
          releaseType = "rotl",
          arrivalDate = LocalDate.parse("2023-04-17"),
          data = updatedData,
          isInapplicable = false,
        ),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success

      val approvedPremisesApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity

      assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
      assertThat(approvedPremisesApplication.isWomensApplication).isEqualTo(false)
      assertThat(approvedPremisesApplication.isPipeApplication).isEqualTo(true)
      assertThat(approvedPremisesApplication.releaseType).isEqualTo("rotl")
      assertThat(approvedPremisesApplication.isInapplicable).isEqualTo(false)
      assertThat(approvedPremisesApplication.arrivalDate).isEqualTo(OffsetDateTime.parse("2023-04-17T00:00:00Z"))
    }
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = "{}",
      ) is AuthorisableActionResult.NotFound,
    ).isTrue
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns Unauthorised when application doesn't belong to request user`() {
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

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    assertThat(
      applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = "{}",
      ) is AuthorisableActionResult.Unauthorised,
    ).isTrue
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application schema is outdated`() {
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
      .withSubmittedAt(null)
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = false
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application has already been submitted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns Success with updated Application`() {
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

    val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()
    val updatedData = """
      {
        "aProperty": "value"
      }
    """

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
    every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns newestSchema
    every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = updatedData,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.Success

    val approvedPremisesApplication = validatableActionResult.entity as TemporaryAccommodationApplicationEntity

    assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
  }

  @Nested
  inner class SubmitApplicationCas1 {
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

    private var submitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
      translatedDocument = {},
      isPipeApplication = true,
      isWomensApplication = false,
      isEmergencyApplication = false,
      isEsapApplication = false,
      targetLocation = "SW1A 1AA",
      releaseType = ReleaseTypeOption.licence,
      type = "CAS1",
      sentenceType = SentenceTypeOption.nonStatutory,
    )

    private val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    @BeforeEach
    fun setup() {
      every { mockObjectMapper.writeValueAsString(submitApprovedPremisesApplication.translatedDocument) } returns "{}"
    }

    @Test
    fun `submitApprovedPremisesApplication returns NotFound when application doesn't exist`() {
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns null

      assertThat(
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          submitApprovedPremisesApplication,
          username,
          "jwt",
          null,
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `submitApprovedPremisesApplication returns Unauthorised when application doesn't belong to request user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser { UserEntityFactory().withDefaultProbationRegion().produce() }
        .produce()

      every { mockUserService.getUserForRequest() } returns UserEntityFactory()
        .withDeliusUsername(username)
        .withDefaultProbationRegion()
        .produce()
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      assertThat(
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          submitApprovedPremisesApplication,
          username,
          "jwt",
          null,
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `submitApprovedPremisesApplication returns GeneralValidationError when application schema is outdated`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        submitApprovedPremisesApplication,
        username,
        "jwt",
        null,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `submitApprovedPremisesApplication returns GeneralValidationError when application has already been submitted`() {
      val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        submitApprovedPremisesApplication,
        username,
        "jwt",
        null,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @ParameterizedTest
    @EnumSource(value = SituationOption::class)
    @NullSource
    @Suppress("CyclomaticComplexMethod")
    fun `submitApprovedPremisesApplication returns Success, creates assessment and stores event, sends confirmation email`(
      situation: SituationOption?,
    ) {
      submitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = situation,
      )

      every { mockObjectMapper.writeValueAsString(submitApprovedPremisesApplication.translatedDocument) } returns "{}"

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockOffenderService.getInmateDetailByNomsNumber(any(), any()) } returns AuthorisableActionResult.Success(
        InmateDetailFactory().withInOutStatus(InOutStatus.OUT).produce(),
      )

      every { mockApAreaRepository.findByIdOrNull(any()) } returns null

      every { mockAssessmentService.createApprovedPremisesAssessment(application) } returns ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          submitApprovedPremisesApplication,
          username,
          "jwt",
        )
      } returns Unit

      every { mockEmailNotificationService.sendEmail(any(), any(), any()) } just Runs

      val result =
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          submitApprovedPremisesApplication,
          username,
          "jwt",
          user.probationRegion.id,
        )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity
      assertThat(persistedApplication.isPipeApplication).isTrue
      assertThat(persistedApplication.isWomensApplication).isFalse
      assertThat(persistedApplication.releaseType).isEqualTo(submitApprovedPremisesApplication.releaseType.toString())
      if (situation == null) {
        assertThat(persistedApplication.situation).isNull()
      } else {
        assertThat(persistedApplication.situation).isEqualTo(situation.toString())
      }
      assertThat(persistedApplication.targetLocation).isEqualTo(submitApprovedPremisesApplication.targetLocation)
      assertThat(persistedApplication.inmateInOutStatusOnSubmission).isEqualTo("OUT")

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockAssessmentService.createApprovedPremisesAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          submitApprovedPremisesApplication,
          username,
          "jwt",
        )
      }

      verify(exactly = 1) {
        mockEmailNotificationService.sendEmail(
          any(),
          "c9944bd8-63c4-473c-8dce-b3636e47d3dd",
          match {
            it["name"] == user.name &&
              (it["applicationUrl"] as String).matches(Regex("http://frontend/applications/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"))
          },
        )
      }
    }
  }

  @SuppressWarnings("UnusedPrivateProperty")
  @Nested
  inner class SubmitApplicationCas3 {
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

    private val submitTemporaryAccommodationApplication = SubmitTemporaryAccommodationApplication(
      translatedDocument = {},
      type = "CAS3",
      arrivalDate = LocalDate.now(),
      summaryData = {
        val num = 50
        val text = "Hello world!"
      },
    )

    private val submitTemporaryAccommodationApplicationWithMiReportingData = SubmitTemporaryAccommodationApplication(
      translatedDocument = {},
      type = "CAS3",
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
      isApplicationEligible = true,
      eligibilityReason = "homelessFromApprovedPremises",
      dutyToReferLocalAuthorityAreaName = "Aberdeen City",
      personReleaseDate = LocalDate.now().plusDays(1),
    )

    @BeforeEach
    fun setup() {
      every { mockObjectMapper.writeValueAsString(submitTemporaryAccommodationApplication.translatedDocument) } returns "{}"
      every { mockObjectMapper.writeValueAsString(submitTemporaryAccommodationApplicationWithMiReportingData.translatedDocument) } returns "{}"
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns null

      assertThat(
        applicationService.submitTemporaryAccommodationApplication(
          applicationId,
          submitTemporaryAccommodationApplication,
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns Unauthorised when application doesn't belong to request user`() {
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
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      assertThat(
        applicationService.submitTemporaryAccommodationApplication(
          applicationId,
          submitTemporaryAccommodationApplication,
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns GeneralValidationError when application schema is outdated`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns GeneralValidationError when application has already been submitted`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns Success and creates assessment`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplication.summaryData!!,
        )
      } returns TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
        .produce()

      every { mockCas3DomainEventService.saveReferralSubmittedEvent(any()) } just Runs

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as TemporaryAccommodationApplicationEntity
      assertThat(persistedApplication.arrivalDate).isEqualTo(
        OffsetDateTime.of(
          submitTemporaryAccommodationApplication.arrivalDate,
          LocalTime.MIDNIGHT,
          ZoneOffset.UTC,
        ),
      )

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplication.summaryData!!,
        )
      }
      verify { mockDomainEventService wasNot called }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveReferralSubmittedEvent(application)
      }
    }

    @Test
    fun `submitTemporaryAccommodationApplication records MI reporting data when supplied`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
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

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplicationWithMiReportingData,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as TemporaryAccommodationApplicationEntity
      assertThat(persistedApplication.arrivalDate).isEqualTo(
        OffsetDateTime.of(
          submitTemporaryAccommodationApplication.arrivalDate,
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

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplicationWithMiReportingData.summaryData!!,
        )
      }
      verify { mockDomainEventService wasNot called }
    }
  }

  @Test
  fun `Get all offline applications where Probation Officer with provided distinguished name does not exist returns empty list`() {
    val distinguishedName = "SOMEPERSON"

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns null

    assertThat(
      applicationService.getAllOfflineApplicationsForUsername(
        distinguishedName,
        ServiceName.approvedPremises,
      ),
    ).isEmpty()
  }

  @Test
  fun `Get all offline applications where Probation Officer exists returns empty list for user without any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`() {
    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    val offlineApplicationEntities = listOf(
      OfflineApplicationEntityFactory()
        .produce(),
      OfflineApplicationEntityFactory()
        .produce(),
      OfflineApplicationEntityFactory()
        .produce(),
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockOfflineApplicationRepository.findAllByService("approved-premises") } returns offlineApplicationEntities

    offlineApplicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(
      applicationService.getAllOfflineApplicationsForUsername(
        distinguishedName,
        ServiceName.approvedPremises,
      ),
    ).isEmpty()
  }

  @ParameterizedTest
  @EnumSource(
    value = UserRole::class,
    names = ["CAS1_WORKFLOW_MANAGER", "CAS1_ASSESSOR", "CAS1_MATCHER", "CAS1_MANAGER"],
  )
  fun `Get all offline applications where Probation Officer exists returns repository results for user with any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`(
    role: UserRole,
  ) {
    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(role)
          .produce()
      }
    val offlineApplicationEntities = listOf(
      OfflineApplicationEntityFactory()
        .produce(),
      OfflineApplicationEntityFactory()
        .produce(),
      OfflineApplicationEntityFactory()
        .produce(),
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockOfflineApplicationRepository.findAllByService("approved-premises") } returns offlineApplicationEntities

    offlineApplicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(
      applicationService.getAllOfflineApplicationsForUsername(
        distinguishedName,
        ServiceName.approvedPremises,
      ),
    ).containsAll(offlineApplicationEntities)
  }

  @Test
  fun `getOfflineApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.getOfflineApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is AuthorisableActionResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getOfflineApplicationForUsername where where caller is not one of one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns OfflineApplicationEntityFactory()
      .produce()

    assertThat(
      applicationService.getOfflineApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is AuthorisableActionResult.Unauthorised,
    ).isTrue
  }

  @ParameterizedTest
  @EnumSource(
    value = UserRole::class,
    names = ["CAS1_WORKFLOW_MANAGER", "CAS1_ASSESSOR", "CAS1_MATCHER", "CAS1_MANAGER"],
  )
  fun `getOfflineApplicationForUsername where user has one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER but does not pass LAO check returns Unauthorised result`(
    role: UserRole,
  ) {
    val distinguishedName = "SOMEPERSON"
    val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(role)
          .produce()
      }

    val applicationEntity = OfflineApplicationEntityFactory()
      .produce()

    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockOffenderService.canAccessOffender(distinguishedName, applicationEntity.crn) } returns false

    val result = applicationService.getOfflineApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOfflineApplicationForUsername where user has any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER and passes LAO check returns Success result with entity from db`() {
    listOf(
      UserRole.CAS1_WORKFLOW_MANAGER,
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_MATCHER,
      UserRole.CAS1_MANAGER,
    ).forEach { role ->
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
        .apply {
          roles += UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(role)
            .produce()
        }

      val applicationEntity = OfflineApplicationEntityFactory()
        .produce()

      every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
      every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
      every { mockOffenderService.canAccessOffender(distinguishedName, applicationEntity.crn) } returns true

      val result = applicationService.getOfflineApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(applicationEntity)
    }
  }

  @Nested
  inner class WithdrawApprovedPremisesApplication {

    @Test
    fun `withdrawApprovedPremisesApplication returns NotFound if Application does not exist`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val applicationId = UUID.fromString("bb13d346-f278-43d7-9c23-5c4077c031ca")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      val result = applicationService.withdrawApprovedPremisesApplication(
        applicationId,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns GeneralValidationError if Application is not AP Application`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = applicationService.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val generalValidationError = (result as CasResult.GeneralValidationError).message

      assertThat(generalValidationError).isEqualTo("onlyCas1Supported")
    }

    @Test
    fun `withdrawApprovedPremisesApplication is idempotent and returns success if already withdrawn`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result =
        applicationService.withdrawApprovedPremisesApplication(application.id, user, "other", null)

      assertThat(result is CasResult.Success).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, emits domain event`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val domainEventWithdrawnBy = WithdrawnByFactory().produce()
      every { mockDomainEventTransformer.toWithdrawnBy(user) } returns domainEventWithdrawnBy
      every { mockDomainEventService.saveApplicationWithdrawnEvent(any()) } just Runs
      every { mockEmailNotificationService.sendEmail(any(), any(), any()) } just Runs

      val result = applicationService.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        mockApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required"
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationWithdrawnEvent(
          match {
            val data = (it.data as ApplicationWithdrawnEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = application.crn,
              noms = application.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawalReason == "alternative_identified_placement_no_longer_required"
          },
        )
      }
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, emits domain event when other reason is set`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      val domainEventWithdrawnBy = WithdrawnByFactory().produce()
      every { mockDomainEventTransformer.toWithdrawnBy(user) } returns domainEventWithdrawnBy
      every { mockDomainEventService.saveApplicationWithdrawnEvent(any()) } just Runs
      every { mockEmailNotificationService.sendEmail(any(), any(), any()) } just Runs

      val result =
        applicationService.withdrawApprovedPremisesApplication(application.id, user, "other", "Some other reason")

      assertThat(result is CasResult.Success).isTrue

      verify {
        mockApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "other" &&
              it.otherWithdrawalReason == "Some other reason"
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationWithdrawnEvent(
          match {
            val data = (it.data as ApplicationWithdrawnEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = application.crn,
              noms = application.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawalReason == "other" &&
              data.otherWithdrawalReason == "Some other reason"
          },
        )
      }
    }

    @Test
    fun `withdrawApprovedPremisesApplication does not persist otherWithdrawalReason if withdrawlReason is not other`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

      every { mockEmailNotificationService.sendEmail(any(), any(), any()) } just Runs

      val domainEventWithdrawnBy = WithdrawnByFactory().produce()
      every { mockDomainEventTransformer.toWithdrawnBy(user) } returns domainEventWithdrawnBy
      every { mockDomainEventService.saveApplicationWithdrawnEvent(any()) } just Runs

      applicationService.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        "Some other reason",
      )

      verify {
        mockApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
              it.otherWithdrawalReason == null
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationWithdrawnEvent(
          match {
            val data = (it.data as ApplicationWithdrawnEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = application.crn,
              noms = application.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
              data.otherWithdrawalReason == null &&
              data.withdrawnBy == domainEventWithdrawnBy
          },
        )
      }
    }
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    @Test
    fun `getWithdrawableState withdrawable if application not withdrawn`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = applicationService.getWithdrawableState(application, user)

      assertThat(result.withdrawable).isTrue()
    }

    @Test
    fun `getWithdrawableState not withdrawable if application already withdrawn `() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = applicationService.getWithdrawableState(application, user)

      assertThat(result.withdrawable).isFalse()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns canWithdraw

      val result = applicationService.getWithdrawableState(application, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
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
    inOutStatus: InOutStatus,
    agencyName: String?,
  ) = InmateDetail(
    offenderNo = "NOMS321",
    inOutStatus = inOutStatus,
    assignedLivingUnit = agencyName?.let {
      AssignedLivingUnit(
        agencyId = "BRI",
        locationId = 5,
        description = "B-2F-004",
        agencyName = it,
      )
    },
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
