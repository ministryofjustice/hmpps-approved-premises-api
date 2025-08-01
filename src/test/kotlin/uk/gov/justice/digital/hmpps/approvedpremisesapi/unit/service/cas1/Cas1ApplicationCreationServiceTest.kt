package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

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
import org.junit.jupiter.params.provider.NullSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService.Cas1ApplicationUpdateFields
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SuppressWarnings("LargeClass")
class Cas1ApplicationCreationServiceTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockOffenderRisksService = mockk<OffenderRisksService>()
  private val mockCas1AssessmentService = mockk<Cas1AssessmentService>()
  private val mockOfflineApplicationRepository = mockk<OfflineApplicationRepository>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockApplicationTeamCodeRepository = mockk<ApplicationTeamCodeRepository>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockApAreaRepository = mockk<ApAreaRepository>()
  private val mockCas1ApplicationDomainEventService = mockk<Cas1ApplicationDomainEventService>()
  private val mockCas1ApplicationUserDetailsRepository = mockk<Cas1ApplicationUserDetailsRepository>()
  private val mockCas1ApplicationEmailService = mockk<Cas1ApplicationEmailService>()
  private val mockPlacementApplicationPlaceholderRepository = mockk<PlacementApplicationPlaceholderRepository>()
  private val mockCas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val mockLockableApplicationRepository = mockk<LockableApplicationRepository>()
  private val mockCas1CruManagementAreaRepository = mockk<Cas1CruManagementAreaRepository>()
  private val mockCas1OffenderService = mockk<Cas1OffenderService>()
  private val mockOffenderDetailService = mockk<OffenderDetailService>()

  private val applicationService = Cas1ApplicationCreationService(
    mockApplicationRepository,
    mockOffenderRisksService,
    mockCas1AssessmentService,
    mockOfflineApplicationRepository,
    mockApDeliusContextApiClient,
    mockApplicationTeamCodeRepository,
    mockObjectMapper,
    mockApAreaRepository,
    mockCas1ApplicationDomainEventService,
    mockCas1ApplicationUserDetailsRepository,
    mockCas1ApplicationEmailService,
    mockPlacementApplicationPlaceholderRepository,
    mockCas1ApplicationStatusService,
    Clock.systemDefaultZone(),
    mockLockableApplicationRepository,
    mockCas1CruManagementAreaRepository,
    mockCas1OffenderService,
    mockOffenderDetailService,
  )

  @Nested
  inner class CreateApprovedPremisesApplication {

    @Test
    fun `Returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
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

      val result = applicationService.createApprovedPremisesApplication(offenderDetails, user, null, null, null)

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.convictionId", "empty")
        .hasMessage("$.deliusEventNumber", "empty")
        .hasMessage("$.offenceId", "empty")
    }

    @Test
    fun `Returns Success with created Application, persists Risk data and Offender name`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      val user = userWithUsername(username)

      every { mockApDeliusContextApiClient.getTeamsManagingCase(crn) } returns ClientResult.Success(
        HttpStatus.OK,
        ManagingTeamsResponse(
          teamCodes = listOf("TEAMCODE"),
        ),
      )

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .produce()

      val cas1OffenderEntityId = UUID.randomUUID()

      val cas1OffenderEntity = Cas1OffenderEntity(
        crn = "CRN345",
        name = "name",
        nomsNumber = "nomsNo",
        tier = "level",
        id = cas1OffenderEntityId,
        createdAt = OffsetDateTime.of(2025, 3, 5, 10, 30, 0, 0, ZoneOffset.UTC),
        lastUpdatedAt = OffsetDateTime.of(2025, 3, 5, 10, 30, 0, 0, ZoneOffset.UTC),
      )

      every { mockApplicationRepository.saveAndFlush(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockApplicationTeamCodeRepository.save(any()) } answers { it.invocation.args[0] as ApplicationTeamCodeEntity }
      every { mockCas1OffenderService.getOrCreateOffender(any(), any()) } returns cas1OffenderEntity

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

      val result = applicationService.createApprovedPremisesApplication(offenderDetails, user, 123, "1", "A12HI")

      assertThatCasResult(result).isSuccess().with {
        val approvedPremisesApplication = it
        assertThat(approvedPremisesApplication.riskRatings).isEqualTo(riskRatings)
        assertThat(approvedPremisesApplication.name).isEqualTo("${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}")
        assertThat(approvedPremisesApplication.cas1OffenderEntity).isEqualTo(cas1OffenderEntity)
      }
    }
  }

  @Nested
  inner class UpdateApplication {

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

    val application = ApprovedPremisesApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByUser(user)
      .withCreatedAt(OffsetDateTime.now())
      .produce()

    @BeforeEach
    fun setupLockMock() {
      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
    }

    @Test
    fun `returns NotFound when application doesn't exist`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThatCasResult(
        applicationService.updateApplication(
          applicationId = applicationId,
          Cas1ApplicationUpdateFields(
            isWomensApplication = false,
            isEmergencyApplication = false,
            apType = null,
            releaseType = null,
            arrivalDate = null,
            data = "{}",
            isInapplicable = null,
            noticeType = Cas1ApplicationTimelinessCategory.standard,
          ),
          userForRequest = user,
        ),
      ).isNotFound(expectedEntityType = "Application", expectedId = applicationId)
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val otherUser = UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce()

      assertThatCasResult(
        applicationService.updateApplication(
          applicationId = applicationId,
          Cas1ApplicationUpdateFields(
            isWomensApplication = false,
            isEmergencyApplication = false,
            apType = null,
            releaseType = null,
            arrivalDate = null,
            data = "{}",
            isInapplicable = null,
            noticeType = Cas1ApplicationTimelinessCategory.standard,
          ),
          userForRequest = otherUser,
        ),
      ).isUnauthorised()
    }

    @Test
    fun `returns GeneralValidationError when application has already been submitted`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      application.submittedAt = OffsetDateTime.now()

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isEmergencyApplication = false,
          apType = null,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
          noticeType = Cas1ApplicationTimelinessCategory.emergency,
        ),
        userForRequest = user,
      )

      assertThatCasResult(result).isGeneralValidationError("This application has already been submitted")
    }

    @EnumSource(
      value = ApprovedPremisesApplicationStatus::class,
      mode = EnumSource.Mode.EXCLUDE,
      names = [ "STARTED", "INAPPLICABLE" ],
    )
    @ParameterizedTest
    fun `returns GeneralValidationError when application doesn't not have a suitable state`(status: ApprovedPremisesApplicationStatus) {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      application.status = status

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isEmergencyApplication = false,
          apType = null,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
          noticeType = Cas1ApplicationTimelinessCategory.emergency,
        ),
        userForRequest = user,
      )

      assertThatCasResult(result).isGeneralValidationError("An application with the status $status cannot be updated.")
    }

    @ParameterizedTest
    @EnumSource(ApType::class)
    fun `Success with updated Application when using apType`(apType: ApType) {
      setupMocksForSuccess()

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isEmergencyApplication = false,
          apType = apType,
          releaseType = "rotl",
          arrivalDate = LocalDate.parse("2023-04-17"),
          data = updatedData,
          isInapplicable = false,
          noticeType = Cas1ApplicationTimelinessCategory.emergency,
        ),
        userForRequest = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.data).isEqualTo(updatedData)
        assertThat(it.isWomensApplication).isEqualTo(false)
        assertThat(it.isPipeApplication).isEqualTo(apType == ApType.pipe)
        assertThat(it.isEsapApplication).isEqualTo(apType == ApType.esap)
        assertThat(it.apType).isEqualTo(apType.asApprovedPremisesType())
        assertThat(it.releaseType).isEqualTo("rotl")
        assertThat(it.isInapplicable).isEqualTo(false)
        assertThat(it.arrivalDate).isEqualTo(OffsetDateTime.parse("2023-04-17T00:00:00Z"))
        assertThat(it.applicantUserDetails).isNull()
        assertThat(it.caseManagerIsNotApplicant).isNull()
        assertThat(it.caseManagerUserDetails).isNull()
        assertThat(it.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.emergency)

        verify { mockCas1ApplicationStatusService.unsubmittedApplicationUpdated(it) }
      }
    }

    @ParameterizedTest
    @EnumSource(value = Cas1ApplicationTimelinessCategory::class)
    fun `sets noticeType correctly`(noticeType: Cas1ApplicationTimelinessCategory) {
      setupMocksForSuccess()

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isEmergencyApplication = noticeType == Cas1ApplicationTimelinessCategory.emergency,
          apType = ApType.pipe,
          releaseType = "rotl",
          arrivalDate = if (noticeType == Cas1ApplicationTimelinessCategory.shortNotice) {
            LocalDate.now().plusDays(10)
          } else {
            LocalDate.now().plusMonths(7)
          },
          data = updatedData,
          isInapplicable = false,
          noticeType = null,
        ),
        userForRequest = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.noticeType).isEqualTo(noticeType)

        verify { mockCas1ApplicationStatusService.unsubmittedApplicationUpdated(it) }
      }
    }

    private fun setupMocksForSuccess() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockCas1ApplicationStatusService.unsubmittedApplicationUpdated(any()) } returns Unit
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    }
  }

  @Nested
  inner class SubmitApplication {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val apArea = ApAreaEntityFactory().produce()
    val user = UserEntityFactory()
      .withDefaults()
      .withDeliusUsername(this.username)
      .withApArea(apArea)
      .produce()

    private var defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
      translatedDocument = {},
      isWomensApplication = false,
      isEmergencyApplication = false,
      targetLocation = "SW1A 1AA",
      releaseType = ReleaseTypeOption.licence,
      type = "CAS1",
      sentenceType = SentenceTypeOption.nonStatutory,
      applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
      caseManagerIsNotApplicant = false,
      apType = ApType.normal,
    )

    @BeforeEach
    fun setup() {
      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"
    }

    @Test
    fun `Returns NotFound when application doesn't exist`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThatCasResult(
        applicationService.submitApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = UUID.randomUUID(),
        ),
      ).isNotFound(expectedEntityType = "ApprovedPremisesApplicationEntity", expectedId = applicationId)
    }

    @Test
    fun `Returns Unauthorised when application doesn't belong to request user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser { UserEntityFactory().withDefaultProbationRegion().produce() }
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      assertThatCasResult(
        applicationService.submitApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = UUID.randomUUID(),
        ),
      ).isUnauthorised()
    }

    @Test
    fun `Returns GeneralValidationError when application has already been submitted`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = applicationService.submitApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThatCasResult(result).isGeneralValidationError("This application has already been submitted")
    }

    @EnumSource(
      value = ApprovedPremisesApplicationStatus::class,
      mode = EnumSource.Mode.EXCLUDE,
      names = [ "STARTED" ],
    )
    @ParameterizedTest
    fun `Returns GeneralValidationError when application doesn't have status 'STARTED'`(state: ApprovedPremisesApplicationStatus) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withStatus(state)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      val result = applicationService.submitApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThatCasResult(result).isGeneralValidationError("Only an application with the 'STARTED' status can be submitted")
    }

    @Test
    fun `Returns GeneralValidationError when applicantIsNotCaseManager is true and no case manager details are provided`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.normal,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
      )

      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"

      val result = applicationService.submitApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThatCasResult(result).isGeneralValidationError("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true")
    }

    @ParameterizedTest
    @EnumSource(value = SituationOption::class)
    @NullSource
    @Suppress("CyclomaticComplexMethod")
    fun `Returns Success, creates assessment and stores event, triggers email`(
      situation: SituationOption?,
    ) {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.pipe,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = situation,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
        noticeType = Cas1ApplicationTimelinessCategory.standard,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      setupMocksForSuccess(application)

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result =
        applicationService.submitApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.isPipeApplication).isTrue
        assertThat(it.isWomensApplication).isFalse
        assertThat(it.releaseType).isEqualTo(defaultSubmitApprovedPremisesApplication.releaseType.toString())
        if (situation == null) {
          assertThat(it.situation).isNull()
        } else {
          assertThat(it.situation).isEqualTo(situation.toString())
        }
        assertThat(it.targetLocation).isEqualTo(defaultSubmitApprovedPremisesApplication.targetLocation)
        assertThat(it.inmateInOutStatusOnSubmission).isEqualTo("OUT")
        assertThat(it.applicantUserDetails).isEqualTo(theApplicantUserDetailsEntity)
        assertThat(it.caseManagerIsNotApplicant).isEqualTo(true)
        assertThat(it.caseManagerUserDetails).isEqualTo(theCaseManagerUserDetailsEntity)
        assertThat(it.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
        assertThat(it.apArea).isEqualTo(apArea)
        assertThat(it.cruManagementArea).isEqualTo(apArea.defaultCruManagementArea)
        assertThat(it.licenceExpiryDate).isNull()
      }

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockCas1AssessmentService.createAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      }

      verify(exactly = 1) { mockCas1ApplicationEmailService.applicationSubmitted(application) }
      verify(exactly = 0) { mockPlacementApplicationPlaceholderRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Cas1ApplicationTimelinessCategory::class)
    fun `Sets noticeType correctly`(noticeType: Cas1ApplicationTimelinessCategory) {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.pipe,
        isWomensApplication = false,
        isEmergencyApplication = noticeType == Cas1ApplicationTimelinessCategory.emergency,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
        arrivalDate = if (noticeType == Cas1ApplicationTimelinessCategory.shortNotice) {
          LocalDate.now().plusDays(10)
        } else {
          LocalDate.now().plusMonths(7)
        },
        noticeType = null,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now())
        .withSubmittedAt(null)
        .produce()

      setupMocksForSuccess(application)

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result =
        applicationService.submitApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.isPipeApplication).isTrue
        assertThat(it.isWomensApplication).isFalse
        assertThat(it.releaseType).isEqualTo(defaultSubmitApprovedPremisesApplication.releaseType.toString())
        assertThat(it.noticeType).isEqualTo(noticeType)
        assertThat(it.targetLocation).isEqualTo(defaultSubmitApprovedPremisesApplication.targetLocation)
        assertThat(it.inmateInOutStatusOnSubmission).isEqualTo("OUT")
        assertThat(it.applicantUserDetails).isEqualTo(theApplicantUserDetailsEntity)
        assertThat(it.caseManagerIsNotApplicant).isEqualTo(true)
        assertThat(it.caseManagerUserDetails).isEqualTo(theCaseManagerUserDetailsEntity)
      }

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockCas1AssessmentService.createAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      }

      verify(exactly = 1) { mockCas1ApplicationEmailService.applicationSubmitted(application) }
      verify(exactly = 1) { mockPlacementApplicationPlaceholderRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(ApType::class)
    fun `Sets apType correctly`(apType: ApType) {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isWomensApplication = false,
        isEmergencyApplication = false,
        apType = apType,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
        arrivalDate = LocalDate.now().plusMonths(7),
        noticeType = null,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now())
        .withSubmittedAt(null)
        .produce()

      setupMocksForSuccess(application)

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result =
        applicationService.submitApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.isPipeApplication).isEqualTo(apType == ApType.pipe)
        assertThat(it.isEsapApplication).isEqualTo(apType == ApType.esap)
        assertThat(it.apType).isEqualTo(apType.asApprovedPremisesType())
        assertThat(it.isWomensApplication).isFalse
        assertThat(it.releaseType).isEqualTo(defaultSubmitApprovedPremisesApplication.releaseType.toString())
        assertThat(it.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
        assertThat(it.targetLocation).isEqualTo(defaultSubmitApprovedPremisesApplication.targetLocation)
        assertThat(it.inmateInOutStatusOnSubmission).isEqualTo("OUT")
        assertThat(it.applicantUserDetails).isEqualTo(theApplicantUserDetailsEntity)
        assertThat(it.caseManagerIsNotApplicant).isEqualTo(true)
        assertThat(it.caseManagerUserDetails).isEqualTo(theCaseManagerUserDetailsEntity)
      }

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockCas1AssessmentService.createAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      }

      verify(exactly = 1) { mockCas1ApplicationEmailService.applicationSubmitted(application) }
    }

    @Test
    fun `Updates existing application user details`() {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.pipe,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      setupMocksForSuccess(application)

      val existingApplicantUserDetails = Cas1ApplicationUserDetailsEntity(
        UUID.randomUUID(),
        "oldApplicantEmail",
        "oldApplicantName",
        "oldApplicantPhone",
      )
      val existingCaseManagerUserDetails = Cas1ApplicationUserDetailsEntity(
        UUID.randomUUID(),
        "oldApplicantEmail",
        "oldApplicantName",
        "oldApplicantPhone",
      )

      val theUpdatedApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match {
            it.id == existingApplicantUserDetails.id &&
              it.name == "applicantName" &&
              it.email == "applicantEmail" &&
              it.telephoneNumber == "applicantPhone"
          },
        )
      } returns theUpdatedApplicantUserDetailsEntity

      val theUpdatedCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match {
            it.id == existingCaseManagerUserDetails.id &&
              it.name == "caseManagerName" &&
              it.email == "caseManagerEmail" &&
              it.telephoneNumber == "caseManagerPhone"
          },
        )
      } returns theUpdatedCaseManagerUserDetailsEntity

      application.applicantUserDetails = existingApplicantUserDetails
      application.caseManagerUserDetails = existingCaseManagerUserDetails

      val result =
        applicationService.submitApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.applicantUserDetails).isEqualTo(theUpdatedApplicantUserDetailsEntity)
        assertThat(it.caseManagerIsNotApplicant).isEqualTo(true)
        assertThat(it.caseManagerUserDetails).isEqualTo(theUpdatedCaseManagerUserDetailsEntity)
      }
    }

    @Test
    fun `If applicant is now case manager, removes existing case manager user details`() {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.pipe,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = false,
        caseManagerUserDetails = null,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withApplicantUserDetails(Cas1ApplicationUserDetailsEntity(UUID.randomUUID(), "applicantName", "applicantEmail", "applicantPhone"))
        .withCaseManagerUserDetails(Cas1ApplicationUserDetailsEntity(UUID.randomUUID(), "oldCaseManEmail", "oldCaseManName", "oldCaseManPhone"))
        .produce()

      setupMocksForSuccess(application)

      val existingApplicantUserDetails = application.applicantUserDetails!!
      val existingCaseManagerUserDetails = application.caseManagerUserDetails!!

      every {
        mockCas1ApplicationUserDetailsRepository.save(match { it.id == existingApplicantUserDetails.id })
      } answers { it.invocation.args[0] as Cas1ApplicationUserDetailsEntity }

      every { mockCas1ApplicationUserDetailsRepository.delete(existingCaseManagerUserDetails) } returns Unit

      val result = applicationService.submitApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = apArea.id,
      )

      assertThatCasResult(result).isSuccess()

      verify { mockCas1ApplicationUserDetailsRepository.delete(existingCaseManagerUserDetails) }
    }

    private fun setupMocksForSuccess(application: ApprovedPremisesApplicationEntity) {
      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockOffenderDetailService.getInmateDetailByNomsNumber(any(), any()) } returns AuthorisableActionResult.Success(
        InmateDetailFactory().withCustodyStatus(InmateStatus.OUT).produce(),
      )

      every { mockApAreaRepository.findByIdOrNull(apArea.id) } returns apArea

      every { mockCas1AssessmentService.createAssessment(application) } returns ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      } returns Unit

      every { mockCas1ApplicationEmailService.applicationSubmitted(any()) } just Runs
      every {
        mockPlacementApplicationPlaceholderRepository.save(any())
      } answers { it.invocation.args[0] as PlacementApplicationPlaceholderEntity }
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
}
