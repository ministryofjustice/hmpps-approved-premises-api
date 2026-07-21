package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.TierDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas1RequestForPlacementServiceTest {
  private val applicationService = mockk<Cas1ApplicationService>()
  private val cas1PlacementApplicationService = mockk<Cas1PlacementApplicationService>()
  private val placementRequestService = mockk<Cas1PlacementRequestService>()
  private val requestForPlacementTransformer = mockk<RequestForPlacementTransformer>()
  private val cas1WithdrawableService = mockk<Cas1WithdrawableService>()
  private val cas1SpaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1SpaceBookingTransformer = mockk<Cas1SpaceBookingTransformer>()
  private val cas1ApplicationService = mockk<Cas1ApplicationService>()
  private val caseService = mockk<CaseService>()

  private val cas1RequestForPlacementService = Cas1RequestForPlacementService(
    applicationService,
    cas1PlacementApplicationService,
    placementRequestService,
    requestForPlacementTransformer,
    cas1WithdrawableService,
    cas1SpaceBookingRepository,
    cas1SpaceBookingTransformer,
    caseService,
  )

  @BeforeEach
  fun setupRequestForPlacementTransformerMock() {
    every { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(any(), any()) } returns mockRfp()
    every { requestForPlacementTransformer.transformPlacementRequestEntityToApi(any(), any()) } returns mockRfp()
  }

  private fun mockRfp(): RequestForPlacement {
    val now = Instant.now()
    return RequestForPlacement(
      id = UUID.randomUUID(),
      createdByUserId = user.id,
      createdAt = now,
      canBeDirectlyWithdrawn = true,
      isWithdrawn = false,
      type = RequestForPlacementType.manual,
      placementDates = listOf(PlacementDates(LocalDate.now(), 14)),
      requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = LocalDate.now(),
        arrivalFlexible = null,
        duration = 14,
      ),
      authorisedPlacementPeriod = null,
      status = RequestForPlacementStatus.requestSubmitted,
      submittedAt = now,
      statusSetDate = now.toLocalDate(),
    )
  }

  companion object {
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()
  }

  @Nested
  inner class GetRequestsForPlacementByApplication {
    @Test
    fun `Returns NotFound result if no application with the specified ID was found`() {
      every { applicationService.getApplication(any()) } returns null

      val id = UUID.randomUUID()
      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(id, user)

      assertThatCasResult(result).isNotFound("Application", id)
    }

    @Test
    fun `Returns all placement applications attached to the application with the specified ID`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplications = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produceMany()
        .take(5)
        .toList()

      every { applicationService.getApplication(application.id) } returns application

      every {
        cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
      } returns placementApplications

      placementApplications.forEach {
        every { cas1WithdrawableService.isDirectlyWithdrawable(it, user) } returns true
      }

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns listOf()

      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, user)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).hasSize(placementApplications.size)
      }

      placementApplications.forEach {
        verify(exactly = 1) { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(it, true) }
      }
    }

    @Test
    fun `Populates placements for a placement application from its linked placement request bookings`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val linkedPlacementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withPlacementRequest(linkedPlacementRequest)
        .produce()

      val spaceBooking = mockk<Cas1SpaceBookingEntity>()
      val shortSummary = mockk<Cas1SpaceBookingShortSummary>()

      every { applicationService.getApplication(application.id) } returns application

      every {
        cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
      } returns listOf(placementApplication)

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns listOf()

      every { cas1WithdrawableService.isDirectlyWithdrawable(placementApplication, user) } returns true

      every {
        cas1SpaceBookingRepository.findByPlacementRequestId(linkedPlacementRequest.id)
      } returns listOf(spaceBooking)

      every {
        cas1SpaceBookingTransformer.transformToCas1SpaceBookingShortSummary(spaceBooking)
      } returns shortSummary

      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, user)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).hasSize(1)
        assertThat(it.single().placements).containsExactly(shortSummary)
      }

      verify(exactly = 1) { cas1SpaceBookingRepository.findByPlacementRequestId(linkedPlacementRequest.id) }
    }

    @Test
    fun `Returns all placement requests attached to the application with the specified ID`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementRequests = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .produceMany()
        .take(5)
        .toList()

      every { applicationService.getApplication(application.id) } returns application

      every {
        cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
      } returns listOf()

      every {
        placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
      } returns placementRequests

      placementRequests.forEach {
        every { cas1WithdrawableService.isDirectlyWithdrawable(it, user) } returns true
      }

      every {
        cas1SpaceBookingRepository.findByPlacementRequestId(any())
      } returns emptyList()

      val result = cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, user)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).hasSize(placementRequests.size)
      }

      placementRequests.forEach {
        verify(exactly = 1) { requestForPlacementTransformer.transformPlacementRequestEntityToApi(it, true) }
      }
    }
  }

  @Nested
  inner class GetRequestsForPlacementDurations {
    @Test
    fun `returns not found when application not found for applicationId`() {
      val applicationId = UUID.randomUUID()

      every { applicationService.getApplication(applicationId) } returns null

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(applicationId, mockk<ApType>())

      assertThatCasResult(defaultDuration).isNotFound(
        "Application",
        expectedId = applicationId
      )
    }

    @Test
    fun `returns not found when case not found for crn`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns null

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, mockk<ApType>())

      assertThatCasResult(defaultDuration).isNotFound(
        "Case",
        expectedId = application.crn
      )
    }

    @Test
    fun `returns not found error when case tier is null`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .produce()

      val case = CaseDtoFactory().withTier(null).produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, mockk<ApType>())

      assertThatCasResult(defaultDuration).isNotFound(
        "Version for live tier associated with case CRN",
        expectedId = application.crn
      )
    }

    @Nested
    inner class V2 {
      @ParameterizedTest
      @ValueSource(
        strings = [
          "normal",
          "mhapElliottHouse",
          "mhapStJosephs",
          "rfap",
        ],
      )
      fun `returns duration 12 weeks when apType is normal or mhapElliottHouse or mhapStJosephs or rfap`(apType: String) {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory().withProbationRegion(
              ProbationRegionEntityFactory().produce()
            )
              .produce()
          )
          .produce()

        val case = CaseDtoFactory().withTier(
          TierDtoFactory().withVersion(
            TierVersionDto.V2).produce())
          .produce()

        every { applicationService.getApplication(application.id) } returns application
        every { caseService.getCase(application.crn) } returns case


        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(12).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }

      @Test
      fun `returns duration 26 weeks when apType is pipe`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory().withProbationRegion(
              ProbationRegionEntityFactory().produce()
            )
              .produce()
          )
          .produce()

        val case = CaseDtoFactory().withTier(
          TierDtoFactory().withVersion(
            TierVersionDto.V2).produce())
          .produce()

        every { applicationService.getApplication(application.id) } returns application
        every { caseService.getCase(application.crn) } returns case

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.pipe)

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(26).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }

      @Test
      fun `returns duration 52 weeks when apType is esap`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory().withProbationRegion(
              ProbationRegionEntityFactory().produce()
            )
              .produce()
          )
          .produce()

        val case = CaseDtoFactory().withTier(
          TierDtoFactory().withVersion(
            TierVersionDto.V2).produce())
          .produce()

        every { applicationService.getApplication(application.id) } returns application
        every { caseService.getCase(application.crn) } returns case

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.esap)

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(52).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }
    }

    @Nested
    inner class V3 {
      @Test
      fun `returns duration 26 weeks when apType is pipe`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory().withProbationRegion(
              ProbationRegionEntityFactory().produce()
            )
              .produce()
          )
          .produce()

        val case = CaseDtoFactory().withTier(
          TierDtoFactory().withVersion(
            TierVersionDto.V3).produce())
          .produce()

        every { applicationService.getApplication(application.id) } returns application
        every { caseService.getCase(application.crn) } returns case

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.pipe)

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(26).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }

    @Test
    fun `returns duration 62 weeks when apType is esap`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory().withVersion(
          TierVersionDto.V3).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.esap)

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(62).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "mhapElliottHouse",
        "mhapStJosephs",
      ],
    )
    fun `returns general validation error when apType is mhap st josephs or mhap elliott house and womens`(apType: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withIsWomensApplication(true)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory().withVersion(
          TierVersionDto.V3).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isGeneralValidationError(
        "MHAP not supported for women's applications",
      )
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "mhapElliottHouse",
        "mhapStJosephs",
      ],
    )
    fun `returns duration 26 weeks when apType is mhap st josephs or mhap elliott house and mens`(apType: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory().withVersion(
          TierVersionDto.V3).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(26).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "normal",
        "rfap",
      ],
    )
    fun `returns duration 16 weeks when apType is normal or rfap and womens`(apType: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withIsWomensApplication(true)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory().withVersion(
          TierVersionDto.V3).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(16).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "normal,life,A",
        "normal,life,B",
        "normal,life,C",
        "normal,ipp,A",
        "normal,ipp,B",
        "normal,ipp,C",
        "rfap,life,A",
        "rfap,life,B",
        "rfap,life,C",
        "rfap,ipp,A",
        "rfap,ipp,B",
        "rfap,ipp,C",
      ],
    )
    fun `returns duration 16 weeks when apType is normal or rfap and mens and sentence type is life or ipp and live tier is A, B, or C`(apType: String, sentenceType: String, liveTierScore: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withSentenceType(sentenceType)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore(liveTierScore).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(16).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "normal,life,D",
        "normal,life,E",
        "normal,life,F",
        "normal,life,G",
        "normal,ipp,D",
        "normal,ipp,E",
        "normal,ipp,F",
        "normal,ipp,G",
        "rfap,life,D",
        "rfap,life,E",
        "rfap,life,F",
        "rfap,life,G",
        "rfap,ipp,D",
        "rfap,ipp,E",
        "rfap,ipp,F",
        "rfap,ipp,G",
      ],
    )
    fun `returns general validation error when apType is normal or rfap and mens and sentence type is life or ipp and live tier is D, E, F or G`(apType: String, sentenceType: String, liveTierScore: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withSentenceType(sentenceType)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore(liveTierScore).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isGeneralValidationError(
        "Only tier A, B or C is eligible for life and ipp sentence type",
      )
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "normal,standardDeterminate",
        "normal,extendedDeterminate",
        "normal,communityOrder",
        "normal,bailPlacement",
        "normal,nonStatutory",
        "rfap,standardDeterminate",
        "rfap,extendedDeterminate",
        "rfap,communityOrder",
        "rfap,bailPlacement",
        "rfap,nonStatutory",
      ],
    )
    fun `returns duration 16 weeks when apType is normal or rfap and mens and sentence type is standardDeterminate, extendedDeterminate, communityOrder, bailPlacement or nonStatutory and live tier is A`(apType: String, sentenceType: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withSentenceType(sentenceType)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore("A").produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(16).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "normal,standardDeterminate",
        "normal,extendedDeterminate",
        "normal,communityOrder",
        "normal,bailPlacement",
        "normal,nonStatutory",
        "rfap,standardDeterminate",
        "rfap,extendedDeterminate",
        "rfap,communityOrder",
        "rfap,bailPlacement",
        "rfap,nonStatutory",
      ],
    )
    fun `returns duration 12 weeks when apType is normal or rfap and mens and sentence type is standardDeterminate, extendedDeterminate, communityOrder, bailPlacement or nonStatutory and live tier is B`(apType: String, sentenceType: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withSentenceType(sentenceType)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore("B").produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(12).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "normal,standardDeterminate,C",
        "normal,extendedDeterminate,C",
        "normal,communityOrder,C",
        "normal,bailPlacement,C",
        "normal,nonStatutory,C",
        "rfap,standardDeterminate,C",
        "rfap,extendedDeterminate,C",
        "rfap,communityOrder,C",
        "rfap,bailPlacement,C",
        "rfap,nonStatutory,C",
        "normal,standardDeterminate,D",
        "normal,extendedDeterminate,D",
        "normal,communityOrder,D",
        "normal,bailPlacement,D",
        "normal,nonStatutory,D",
        "rfap,standardDeterminate,D",
        "rfap,extendedDeterminate,D",
        "rfap,communityOrder,D",
        "rfap,bailPlacement,D",
        "rfap,nonStatutory,D",
      ],
    )
    fun `returns duration 8 weeks when apType is normal or rfap and mens and sentence type is standardDeterminate, extendedDeterminate, communityOrder, bailPlacement or nonStatutory and live tier is C or D`(apType: String, sentenceType: String, liveTierScore: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withSentenceType(sentenceType)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore(liveTierScore).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isSuccess().with {
        assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(8).days)
        assertThat(it.maxDurationDays).isNull()
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "normal,standardDeterminate,E",
        "normal,extendedDeterminate,E",
        "normal,communityOrder,E",
        "normal,bailPlacement,E",
        "normal,nonStatutory,E",
        "rfap,standardDeterminate,E",
        "rfap,extendedDeterminate,E",
        "rfap,communityOrder,E",
        "rfap,bailPlacement,E",
        "rfap,nonStatutory,E",
        "normal,standardDeterminate,F",
        "normal,extendedDeterminate,F",
        "normal,communityOrder,F",
        "normal,bailPlacement,F",
        "normal,nonStatutory,F",
        "rfap,standardDeterminate,F",
        "rfap,extendedDeterminate,F",
        "rfap,communityOrder,F",
        "rfap,bailPlacement,F",
        "rfap,nonStatutory,F",
        "normal,standardDeterminate,G",
        "normal,extendedDeterminate,G",
        "normal,communityOrder,G",
        "normal,bailPlacement,G",
        "normal,nonStatutory,G",
        "rfap,standardDeterminate,G",
        "rfap,extendedDeterminate,G",
        "rfap,communityOrder,G",
        "rfap,bailPlacement,G",
        "rfap,nonStatutory,G",
      ],
    )
    fun `returns general validation error when apType is normal or rfap and mens and sentence type is standardDeterminate, extendedDeterminate, communityOrder, bailPlacement or nonStatutory and live tier is E, F or G`(apType: String, sentenceType: String, liveTierScore: String) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory().withProbationRegion(
            ProbationRegionEntityFactory().produce()
          )
            .produce()
        )
        .withSentenceType(sentenceType)
        .produce()

      val case = CaseDtoFactory().withTier(
        TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore(liveTierScore).produce())
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { caseService.getCase(application.crn) } returns case

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType))

      assertThatCasResult(defaultDuration).isGeneralValidationError(
        "Cannot calculate duration for ap type $apType, sentence type ${application.sentenceType}, tier score $liveTierScore",
      )
    }
    }
  }
}
