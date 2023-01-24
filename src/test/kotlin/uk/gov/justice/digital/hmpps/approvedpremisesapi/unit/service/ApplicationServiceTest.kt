package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonLogicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationServiceTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockJsonLogicService = mockk<JsonLogicService>()
  private val mockOfflineApplicationRepository = mockk<OfflineApplicationRepository>()

  private val applicationService = ApplicationService(
    mockUserRepository,
    mockApplicationRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockUserService,
    mockAssessmentService,
    mockJsonLogicService,
    mockOfflineApplicationRepository
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
      .produce()
    val applicationEntities = listOf(
      ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce(),
      ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce(),
      ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce()
    )

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockApplicationRepository.findAllByCreatedByUser_Id(userId, ApprovedPremisesApplicationEntity::class.java) } returns applicationEntities
    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    applicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).containsAll(applicationEntities)
  }

  @Test
  fun `getApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getApplicationForUsername where application was not created by calller and where caller is not one of one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns UserEntityFactory().produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(UserEntityFactory().produce())
      .produce()

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
        .produce()

      val otherUserEntity = UserEntityFactory().produce()

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
  fun `createApplication returns FieldValidationError when CRN does not exist`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.NotFound()

    val result = applicationService.createApplication(crn, username, "jwt", "approved-premises", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "doesNotExist")
  }

  @Test
  fun `createApplication returns FieldValidationError when CRN is LAO restricted`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Unauthorised()

    val result = applicationService.createApplication(crn, username, "jwt", "approved-premises", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "userPermission")
  }

  @Test
  fun `createApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )

    val result = applicationService.createApplication(crn, username, "jwt", "approved-premises", null, null, null)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.convictionId", "empty")
    assertThat(result.validationMessages).containsEntry("$.deliusEventNumber", "empty")
    assertThat(result.validationMessages).containsEntry("$.offenceId", "empty")
  }

  @Test
  fun `createApplication returns Success with created Application + persisted Risk data`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val user = UserEntityFactory().produce()
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )
    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns schema
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

    val result = applicationService.createApplication(crn, username, "jwt", "approved-premises", 123, "1", "A12HI")

    assertThat(result is ValidatableActionResult.Success).isTrue
    result as ValidatableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)
    assertThat(result.entity.createdByUser).isEqualTo(user)
    val approvedPremisesApplication = result.entity as ApprovedPremisesApplicationEntity
    assertThat(approvedPremisesApplication.riskRatings).isEqualTo(riskRatings)
  }

  @Test
  fun `updateApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.updateApplication(applicationId, "{}", username) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `updateApplication returns Unauthorised when application doesn't belong to request user`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val application = ApprovedPremisesApplicationEntityFactory()
      .withId(applicationId)
      .withYieldedCreatedByUser { UserEntityFactory().produce() }
      .produce()

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withDeliusUsername(username)
      .produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    assertThat(applicationService.updateApplication(applicationId, "{}", username) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `updateApplication returns GeneralValidationError when application schema is outdated`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val user = UserEntityFactory()
      .withDeliusUsername(username)
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

    val result = applicationService.updateApplication(applicationId, "{}", username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `updateApplication returns GeneralValidationError when application has already been submitted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
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

    val result = applicationService.updateApplication(applicationId, "{}", username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `updateApplication returns Success with updated Application`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val user = UserEntityFactory()
      .withDeliusUsername(username)
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

    val result = applicationService.updateApplication(applicationId, updatedData, username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.Success

    assertThat(validatableActionResult.entity.data).isEqualTo(updatedData)
  }

  @Test
  fun `submitApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.submitApplication(applicationId, "{}", username) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `submitApplication returns Unauthorised when application doesn't belong to request user`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val application = ApprovedPremisesApplicationEntityFactory()
      .withId(applicationId)
      .withYieldedCreatedByUser { UserEntityFactory().produce() }
      .produce()

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withDeliusUsername(username)
      .produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    assertThat(applicationService.submitApplication(applicationId, "{}", username) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `submitApplication returns GeneralValidationError when application schema is outdated`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val user = UserEntityFactory()
      .withDeliusUsername(username)
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

    val result = applicationService.submitApplication(applicationId, "{}", username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `submitApplication returns GeneralValidationError when application has already been submitted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
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

    val result = applicationService.submitApplication(applicationId, "{}", username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `submitApplication returns Success, runs json logic rules and creates assessment`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .produce()

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
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
    every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockJsonLogicService.resolveBoolean(newestSchema.isPipeJsonLogicRule, application.data!!) } returns true
    every { mockJsonLogicService.resolveBoolean(newestSchema.isWomensJsonLogicRule, application.data!!) } returns false
    every { mockAssessmentService.createAssessment(application) } returns AssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(user)
      .produce()

    val result = applicationService.submitApplication(applicationId, "{}", username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.Success
    val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity
    assertThat(persistedApplication.isPipeApplication).isTrue
    assertThat(persistedApplication.isWomensApplication).isFalse

    verify { mockApplicationRepository.save(any()) }
    verify(exactly = 1) { mockAssessmentService.createAssessment(application) }
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
    every { mockOfflineApplicationRepository.findAllWhereService("approved-premises") } returns offlineApplicationEntities
    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    offlineApplicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(applicationService.getAllOfflineApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).isEmpty()
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "WORKFLOW_MANAGER", "ASSESSOR", "MATCHER", "MANAGER" ])
  fun `Get all offline applications where Probation Officer exists returns repository results for user with any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`(role: UserRole) {
    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
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
    every { mockOfflineApplicationRepository.findAllWhereService("approved-premises") } returns offlineApplicationEntities
    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    offlineApplicationEntities.forEach {
      every { mockOffenderService.canAccessOffender(distinguishedName, it.crn) } returns true
    }

    assertThat(applicationService.getAllOfflineApplicationsForUsername(distinguishedName, ServiceName.approvedPremises)).containsAll(offlineApplicationEntities)
  }
}
