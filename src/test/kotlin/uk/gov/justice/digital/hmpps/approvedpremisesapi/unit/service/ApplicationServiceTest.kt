package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LargeClass")
class ApplicationServiceTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockOffenderRisksService = mockk<OffenderRisksService>()
  private val mockUserService = mockk<UserService>()
  private val mockOfflineApplicationRepository = mockk<OfflineApplicationRepository>()
  private val mockUserAccessService = mockk<UserAccessService>()

  private val applicationService = ApplicationService(
    mockUserRepository,
    mockApplicationRepository,
    mockOffenderService,
    mockOffenderRisksService,
    mockUserService,
    mockOfflineApplicationRepository,
    mockUserAccessService,
  )

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
      object : ApprovedPremisesApplicationSummary {
        override fun getIsWomensApplication(): Boolean? = true
        override fun getIsPipeApplication(): Boolean? = true
        override fun getIsEsapApplication() = true
        override fun getIsEmergencyApplication() = true
        override fun getArrivalDate(): Instant? = null
        override fun getRiskRatings(): String? = null
        override fun getId(): UUID = UUID.fromString("8ecbbd9c-3c66-4f0b-8f21-87f537676422")
        override fun getCrn(): String = "CRN123"
        override fun getCreatedByUserId(): UUID = UUID.fromString("60d0a768-1d05-4538-a6fd-78eb723dd310")
        override fun getCreatedAt(): Instant = Instant.parse("2023-04-20T10:11:00+01:00")
        override fun getSubmittedAt(): Instant? = null
        override fun getTier(): String? = null
        override fun getStatus(): String = ApprovedPremisesApplicationStatus.started.toString()
        override fun getIsWithdrawn(): Boolean = false
        override fun getReleaseType(): String = ReleaseTypeOption.licence.toString()
        override fun getHasRequestsForPlacement(): Boolean = false
      },
    )

    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity
    every { mockApplicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(userId) } returns applicationSummaries

    assertThat(
      applicationService.getAllApplicationsForUsername(
        userEntity = userEntity,
        ServiceName.approvedPremises,
      ),
    ).containsAll(applicationSummaries)
  }

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

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns deletedApplication

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where temporary accommodation application was deleted returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.NotFound,
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
      ) is CasResult.Unauthorised,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where user can access the application returns Success result with entity from db`() {
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

    val applicationEntity = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .produce()

    every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(any()) } returns userEntity
    every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

    val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success

    assertThat(result.value).isEqualTo(applicationEntity)
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Unauthorised when user doesn't have CAS3_REFERRER role`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
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
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN does not exist`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
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
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN is LAO restricted`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
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
  fun `createTemporaryAccommodationApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
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
  fun `createTemporaryAccommodationApplication returns Success with created Application + persisted Risk data`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isEqualTo(agencyName)
    }
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application with prison name when person status is TRN`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isEqualTo(agencyName)
    }
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application without prison name when person status is Out`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
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
  fun `createTemporaryAccommodationApplication returns Success with created Application without prison name when assignedLivingUnit is null`() {
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

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isNull()
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
      ) is CasResult.NotFound,
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

    assertThat(
      applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = "{}",
      ) is CasResult.Unauthorised,
    ).isTrue
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application has already been submitted`() {
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

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError

    assertThat(result.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application has already been deleted`() {
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

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError

    assertThat(result.message).isEqualTo("This application has already been deleted")
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

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = updatedData,
    )

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success

    val approvedPremisesApplication = result.value as TemporaryAccommodationApplicationEntity

    assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
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
      ) is CasResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getOfflineApplicationForUsername where where caller is not one of one of roles CAS1_CRU_MEMBER, ASSESSOR, MATCHER, MANAGER returns Unauthorised result`() {
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
      ) is CasResult.Unauthorised,
    ).isTrue
  }

  @ParameterizedTest
  @EnumSource(
    value = UserRole::class,
    names = ["CAS1_CRU_MEMBER", "CAS1_ASSESSOR", "CAS1_FUTURE_MANAGER"],
  )
  fun `getOfflineApplicationForUsername where user has one of roles CAS1_CRU_MEMBER, ASSESSOR, FUTURE_MANAGER but does not pass LAO check returns Unauthorised result`(
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

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @ParameterizedTest
  @EnumSource(
    value = UserRole::class,
    names = ["CAS1_CRU_MEMBER", "CAS1_ASSESSOR", "CAS1_FUTURE_MANAGER"],
  )
  fun `getOfflineApplicationForUsername where user has permission of roles CAS1_CRU_MEMBER, ASSESSOR, FUTURE_MANAGER and passes LAO check returns Success result with entity from db`(role: UserRole) {
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

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success

    assertThat(result.value).isEqualTo(applicationEntity)
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
