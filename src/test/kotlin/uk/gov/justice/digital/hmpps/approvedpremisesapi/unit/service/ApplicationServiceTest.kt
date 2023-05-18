package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

class ApplicationServiceTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockOfflineApplicationRepository = mockk<OfflineApplicationRepository>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockApplicationTeamCodeRepository = mockk<ApplicationTeamCodeRepository>()
  private val mockObjectMapper = mockk<ObjectMapper>()

  private val applicationService = ApplicationService(
    mockUserRepository,
    mockApplicationRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockUserService,
    mockAssessmentService,
    mockOfflineApplicationRepository,
    mockDomainEventService,
    mockCommunityApiClient,
    mockApDeliusContextApiClient,
    mockApplicationTeamCodeRepository,
    mockObjectMapper,
    "http://frontend/applications/#id"
  )

  @Test
  fun `Get all applications where Probation Officer with provided distinguished name does not exist returns empty list`() {
    val distinguishedName = "SOMEPERSON"

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns null

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).isEmpty()
  }

  @Test
  fun `Get all applications where Probation Officer exists returns applications returned from repository`() {
    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

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
        override fun getHasPlacementRequest(): Boolean = false
        override fun getIsWomensApplication(): Boolean? = true
        override fun getIsPipeApplication(): Boolean? = true
        override fun getArrivalDate(): Timestamp? = null
        override fun getRiskRatings(): String? = null
        override fun getId(): UUID = UUID.fromString("8ecbbd9c-3c66-4f0b-8f21-87f537676422")
        override fun getCrn(): String = "CRN123"
        override fun getCreatedByUserId(): UUID = UUID.fromString("60d0a768-1d05-4538-a6fd-78eb723dd310")
        override fun getCreatedAt(): Timestamp = Timestamp.from(Instant.parse("2023-04-20T10:11:00+01:00"))
        override fun getSubmittedAt(): Timestamp? = null
        override fun getLatestAssessmentSubmittedAt(): Timestamp? = null
        override fun getLatestAssessmentDecision(): AssessmentDecision? = null
        override fun getLatestAssessmentHasClarificationNotesWithoutResponse(): Boolean = false
        override fun getHasBooking(): Boolean = false
      }
    )

    every { mockCommunityApiClient.getStaffUserDetails(distinguishedName) } returns ClientResult.Success(
      HttpStatus.OK,
      StaffUserDetailsFactory()
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockApplicationRepository.findApprovedPremisesSummariesForManagingTeams(listOf("TEAM1")) } returns applicationSummaries
    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    applicationSummaries.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.getCrn()) } returns true
    }

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).containsAll(applicationSummaries)
  }

  @Test
  fun `getApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getApplicationForUsername where application was not created by caller, not in case load and where caller is not one of one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns UserEntityFactory()
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce()
      )
      .produce()

    val staffUserDetails = StaffUserDetailsFactory()
      .withTeams(
        listOf(
          StaffUserTeamMembershipFactory()
            .produce()
        )
      )
      .produce()

    every { mockCommunityApiClient.getStaffUserDetails(distinguishedName) } returns ClientResult.Success(
      status = HttpStatus.OK,
      body = staffUserDetails
    )

    assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getApplicationForUsername where application was created by caller returns Success result with entity from db`() {
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
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity

    val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity).isEqualTo(applicationEntity)
  }

  @Test
  fun `getApplicationForUsername where application CRN is managed by team caller is in returns Success result with entity from db`() {
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

    val otherUserEntity = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val applicationEntity = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(otherUserEntity)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    applicationEntity.teamCodes += ApplicationTeamCodeEntity(
      id = UUID.randomUUID(),
      application = applicationEntity,
      teamCode = "TEAM1"
    )

    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity

    val staffUserDetails = StaffUserDetailsFactory()
      .withTeams(
        listOf(
          StaffUserTeamMembershipFactory()
            .withCode("TEAM1")
            .produce()
        )
      )
      .produce()

    every { mockCommunityApiClient.getStaffUserDetails(distinguishedName) } returns ClientResult.Success(
      status = HttpStatus.OK,
      body = staffUserDetails
    )

    val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity).isEqualTo(applicationEntity)
  }

  @Test
  fun `getApplicationForUsername where application not created by caller but user has any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns Success result with entity from db`() {
    listOf(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER).forEach { role ->
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

      val otherUserEntity = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      userEntity.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(userEntity)
          .withRole(role)
          .produce()
      )

      val applicationEntity = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(otherUserEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce()

      every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
      every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity

      val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(applicationEntity)
    }
  }

  @Test
  fun `createApprovedPremisesApplication returns FieldValidationError when CRN does not exist`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.NotFound()

    val user = userWithUsername(username)

    val result = applicationService.createApprovedPremisesApplication(crn, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "doesNotExist")
  }

  @Test
  fun `createApprovedPremisesApplication returns FieldValidationError when CRN is LAO restricted`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Unauthorised()

    val user = userWithUsername(username)

    val result = applicationService.createApprovedPremisesApplication(crn, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "userPermission")
  }

  @Test
  fun `createApprovedPremisesApplication returns FieldValidationError when CRN is not managed by any teams the user is part of`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )

    val user = userWithUsername(username)

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn, user.deliusStaffCode!!) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = emptyList()
      )
    )

    val result = applicationService.createApprovedPremisesApplication(crn, user, "jwt", null, null, null)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "notInCaseload")
  }

  @Test
  fun `createApprovedPremisesApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )

    val user = userWithUsername(username)

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn, user.deliusStaffCode!!) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAMCODE")
      )
    )

    val result = applicationService.createApprovedPremisesApplication(crn, user, "jwt", null, null, null)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.convictionId", "empty")
    assertThat(result.validationMessages).containsEntry("$.deliusEventNumber", "empty")
    assertThat(result.validationMessages).containsEntry("$.offenceId", "empty")
  }

  @Test
  fun `createApprovedPremisesApplication returns Success with created Application + persisted Risk data`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username)

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn, user.deliusStaffCode!!) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAMCODE")
      )
    )

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )
    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
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
            lastUpdated = null
          )
        )
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12")
          )
        )
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2"
          )
        )
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(riskRatings)

    val result = applicationService.createApprovedPremisesApplication(crn, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.Success).isTrue
    result as ValidatableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)
    assertThat(result.entity.createdByUser).isEqualTo(user)
    val approvedPremisesApplication = result.entity as ApprovedPremisesApplicationEntity
    assertThat(approvedPremisesApplication.riskRatings).isEqualTo(riskRatings)
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN does not exist`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.NotFound()

    val user = userWithUsername(username)

    val result = applicationService.createTemporaryAccommodationApplication(crn, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "doesNotExist")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN is LAO restricted`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Unauthorised()

    val user = userWithUsername(username)

    val result = applicationService.createTemporaryAccommodationApplication(crn, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "userPermission")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )

    val user = userWithUsername(username)

    val result = applicationService.createTemporaryAccommodationApplication(crn, user, "jwt", null, null, null)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.convictionId", "empty")
    assertThat(result.validationMessages).containsEntry("$.deliusEventNumber", "empty")
    assertThat(result.validationMessages).containsEntry("$.offenceId", "empty")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application + persisted Risk data`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username)

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
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
            lastUpdated = null
          )
        )
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12")
          )
        )
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2"
          )
        )
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, "jwt", username) } returns AuthorisableActionResult.Success(riskRatings)

    val result = applicationService.createTemporaryAccommodationApplication(crn, user, "jwt", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.Success).isTrue
    result as ValidatableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)
    assertThat(result.entity.createdByUser).isEqualTo(user)
    val temporaryAccommodationApplication = result.entity as TemporaryAccommodationApplicationEntity
    assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
  }

  @Test
  fun `updateApprovedPremisesApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        isWomensApplication = false,
        isPipeApplication = null,
        releaseType = null,
        arrivalDate = null,
        data = "{}",
        username = username,
        isInapplicable = null
      ) is AuthorisableActionResult.NotFound
    ).isTrue
  }

  @Test
  fun `updateApprovedPremisesApplication returns Unauthorised when application doesn't belong to request user`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val application = ApprovedPremisesApplicationEntityFactory()
      .withId(applicationId)
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce()
      }
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
      applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        isWomensApplication = false,
        isPipeApplication = null,
        releaseType = null,
        arrivalDate = null,
        data = "{}",
        username = username,
        isInapplicable = null
      ) is AuthorisableActionResult.Unauthorised
    ).isTrue
  }

  @Test
  fun `updateApprovedPremisesApplication returns GeneralValidationError when application schema is outdated`() {
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

    val application = ApprovedPremisesApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(null)
      .produce()
      .apply {
        schemaUpToDate = false
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateApprovedPremisesApplication(
      applicationId = applicationId,
      isWomensApplication = false,
      isPipeApplication = null,
      releaseType = null,
      arrivalDate = null,
      data = "{}",
      username = username,
      isInapplicable = null
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `updateApprovedPremisesApplication returns GeneralValidationError when application has already been submitted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

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
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateApprovedPremisesApplication(
      applicationId = applicationId,
      isWomensApplication = false,
      isPipeApplication = null,
      releaseType = null,
      arrivalDate = null,
      data = "{}",
      username = username,
      isInapplicable = null
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `updateApprovedPremisesApplication returns Success with updated Application`() {
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

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
    every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns newestSchema
    every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val result = applicationService.updateApprovedPremisesApplication(
      applicationId = applicationId,
      isWomensApplication = false,
      isPipeApplication = true,
      releaseType = "rotl",
      arrivalDate = LocalDate.parse("2023-04-17"),
      data = updatedData,
      username = username,
      isInapplicable = false
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

  @Test
  fun `updateTemporaryAccommodationApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = "{}",
        username = username
      ) is AuthorisableActionResult.NotFound
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
        username = username
      ) is AuthorisableActionResult.Unauthorised
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
      username = username
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
      username = username
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
      username = username
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.Success

    val approvedPremisesApplication = validatableActionResult.entity as TemporaryAccommodationApplicationEntity

    assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
  }

  @Nested
  inner class SubmitApplication {
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

    private val submitApplication = SubmitApprovedPremisesApplication(
      translatedDocument = {},
      isPipeApplication = true,
      isWomensApplication = false,
      targetLocation = "SW1A 1AA",
      releaseType = ReleaseTypeOption.licence
    )

    @BeforeEach
    fun setup() {
      every { mockObjectMapper.writeValueAsString(submitApplication.translatedDocument) } returns "{}"
    }

    @Test
    fun `submitApprovedPremisesApplication returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns null

      assertThat(applicationService.submitApprovedPremisesApplication(applicationId, submitApplication, username, "jwt") is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `submitApprovedPremisesApplication returns Unauthorised when application doesn't belong to request user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce()
        }
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

      assertThat(applicationService.submitApprovedPremisesApplication(applicationId, submitApplication, username, "jwt") is AuthorisableActionResult.Unauthorised).isTrue
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

      val result = applicationService.submitApprovedPremisesApplication(applicationId, submitApplication, username, "jwt")

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

      val result = applicationService.submitApprovedPremisesApplication(applicationId, submitApplication, username, "jwt")

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `submitApprovedPremisesApplication returns Success, creates assessment and stores event`() {
      val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
      every { mockAssessmentService.createAssessment(application) } returns AssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withGender("male")
        .withCrn(application.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
        offenderDetails
      )

      val risks = PersonRisksFactory()
        .withMappa(
          RiskWithStatus(
            status = RiskStatus.Retrieved,
            value = Mappa(
              level = "CAT C1/LEVEL L1",
              lastUpdated = LocalDate.now()
            )
          )
        )
        .produce()

      every { mockOffenderService.getRiskByCrn(application.crn, any(), user.deliusUsername) } returns AuthorisableActionResult.Success(
        risks
      )

      val staffUserDetails = StaffUserDetailsFactory()
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .produce()
          )
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = staffUserDetails
      )

      val schema = application.schemaVersion as ApprovedPremisesApplicationJsonSchemaEntity

      every { mockDomainEventService.saveApplicationSubmittedDomainEvent(any()) } just Runs

      val result = applicationService.submitApprovedPremisesApplication(applicationId, submitApplication, username, "jwt")

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity
      assertThat(persistedApplication.isPipeApplication).isTrue
      assertThat(persistedApplication.isWomensApplication).isFalse
      assertThat(persistedApplication.releaseType).isEqualTo(submitApplication.releaseType.toString())

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockAssessmentService.createAssessment(application) }

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationSubmittedDomainEvent(
          match {
            val data = (it.data as ApplicationSubmittedEnvelope).eventDetails
            val firstTeam = staffUserDetails.teams!!.first()

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.releaseType == submitApplication.releaseType.toString() &&
              data.age == Period.between(offenderDetails.dateOfBirth, LocalDate.now()).years &&
              data.gender == ApplicationSubmitted.Gender.male &&
              data.submittedBy == ApplicationSubmittedSubmittedBy(
              staffMember = StaffMember(
                staffCode = staffUserDetails.staffCode,
                staffIdentifier = staffUserDetails.staffIdentifier,
                forenames = staffUserDetails.staff.forenames,
                surname = staffUserDetails.staff.surname,
                username = staffUserDetails.username
              ),
              probationArea = ProbationArea(
                code = staffUserDetails.probationArea.code,
                name = staffUserDetails.probationArea.description
              ),
              team = Team(
                code = firstTeam.code,
                name = firstTeam.description
              ),
              ldu = Ldu(
                code = firstTeam.teamType.code,
                name = firstTeam.teamType.description
              ),
              region = Region(
                code = staffUserDetails.probationArea.code,
                name = staffUserDetails.probationArea.description
              )
            ) &&
              data.mappa == risks.mappa.value!!.level &&
              data.sentenceLengthInMonths == null &&
              data.offenceId == application.offenceId
          }
        )
      }
    }
  }
  @Test
  fun `Get all offline applications where Probation Officer with provided distinguished name does not exist returns empty list`() {
    val distinguishedName = "SOMEPERSON"

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns null

    assertThat(applicationService.getAllOfflineApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).isEmpty()
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
        .produce()
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockOfflineApplicationRepository.findAllByService("approved-premises") } returns offlineApplicationEntities

    offlineApplicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(applicationService.getAllOfflineApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).isEmpty()
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["WORKFLOW_MANAGER", "ASSESSOR", "MATCHER", "MANAGER"])
  fun `Get all offline applications where Probation Officer exists returns repository results for user with any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`(role: UserRole) {
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
        .produce()
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockOfflineApplicationRepository.findAllByService("approved-premises") } returns offlineApplicationEntities

    offlineApplicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(applicationService.getAllOfflineApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).containsAll(offlineApplicationEntities)
  }

  @Test
  fun `getOfflineApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.getOfflineApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.NotFound).isTrue
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

    assertThat(applicationService.getOfflineApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["WORKFLOW_MANAGER", "ASSESSOR", "MATCHER", "MANAGER"])
  fun `getOfflineApplicationForUsername where user has one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER but does not pass LAO check returns Unauthorised result`(role: UserRole) {
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
    listOf(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER).forEach { role ->
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

  private fun userWithUsername(username: String) = UserEntityFactory()
    .withDeliusUsername(username)
    .withProbationRegion(
      ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()
    )
    .produce()
}
