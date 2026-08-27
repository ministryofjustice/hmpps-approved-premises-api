package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.commons.csv.CSVFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.TierDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.Cas1DomainEventEnvelopeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.stream.Stream

class Cas1RequestForPlacementServiceTest {
  private val applicationService = mockk<Cas1ApplicationService>()
  private val cas1PlacementApplicationService = mockk<Cas1PlacementApplicationService>()
  private val placementRequestService = mockk<Cas1PlacementRequestService>()
  private val requestForPlacementTransformer = mockk<RequestForPlacementTransformer>()
  private val cas1WithdrawableService = mockk<Cas1WithdrawableService>()
  private val cas1SpaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1SpaceBookingTransformer = mockk<Cas1SpaceBookingTransformer>()
  private val tierService = mockk<TierService>()
  private val domainEventRepository = mockk<DomainEventRepository>()
  private val jsonMapper = JsonMapper()
  private val sentryService = mockk<SentryService>(relaxed = true)

  private val cas1RequestForPlacementService = Cas1RequestForPlacementService(
    applicationService,
    cas1PlacementApplicationService,
    placementRequestService,
    requestForPlacementTransformer,
    cas1WithdrawableService,
    cas1SpaceBookingRepository,
    cas1SpaceBookingTransformer,
    domainEventRepository,
    tierService,
    jsonMapper,
    sentryService,
  )

  @BeforeEach
  fun setupRequestForPlacementTransformerMock() {
    every { requestForPlacementTransformer.transformPlacementApplicationEntityToApi(any(), any()) } returns mockRfp()
    every { requestForPlacementTransformer.transformPlacementRequestEntityToApi(any(), any()) } returns mockRfp()
  }

  private fun mockRfp(): RequestForPlacement {
    val now = Instant.now()
    val requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
      arrival = LocalDate.now(),
      arrivalFlexible = null,
      duration = 14,
    )
    return RequestForPlacement(
      id = UUID.randomUUID(),
      createdByUserId = user.id,
      createdAt = now,
      canBeDirectlyWithdrawn = true,
      isWithdrawn = false,
      type = RequestForPlacementType.manual,
      requestedPlacementPeriod = requestedPlacementPeriod,
      authorisedPlacementPeriod = null,
      status = RequestForPlacementStatus.requestSubmitted,
      submittedAt = now,
      statusSetDate = now.toLocalDate(),
      submittedBy = Cas1StaffDto(
        name = user.name,
        staffCode = user.deliusStaffCode,
        username = user.deliusUsername,
      ),
      decision = null,
      canonicalPlacementPeriod = requestedPlacementPeriod,
    )
  }

  data class DurationCalculationExpectation(
    val fromCsvRow: Long,
    val apType: ApType,
    val tier: String,
    val isWomensApplication: Boolean,
    val sentenceType: SentenceTypeOption,
    val exceptionStatus: Boolean,
    val expectedDurationWeeks: Int?,
  )

  @Nested
  inner class GetRequestForPlacementRejectionReason {
    @Test
    fun `getRequestForPlacementRejectionReason returns null if request for placement event has no rejection reason`() {
      val requestForPlacement = RequestForPlacementFactory().produce()

      every { domainEventRepository.findAssessedRequestForPlacement(requestForPlacement.id) } returns null

      val result = cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement)

      assertThat(result).isNull()
    }

    @Test
    fun `getRequestForPlacementRejectionReason returns rejectionReason if request for placement event has rejection reason`() {
      val requestForPlacement = RequestForPlacementFactory().produce()

      val details = RequestForPlacementAssessed(
        applicationId = UUID.randomUUID(),
        applicationUrl = "url",
        placementApplicationId = requestForPlacement.id,
        assessedBy = StaffMemberFactory().produce(),
        decision = RequestForPlacementAssessed.Decision.rejected,
        expectedArrival = LocalDate.now(),
        duration = 12,
        decisionSummary = "No space",
      )
      val envelopedData = Cas1DomainEventEnvelopeFactory<RequestForPlacementAssessed>()
        .withDetails(details)
        .produce()

      every { domainEventRepository.findAssessedRequestForPlacement(requestForPlacement.id) } returns DomainEventEntityFactory()
        .withData(jsonMapper.writeValueAsString(envelopedData))
        .produce()

      val result = cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement)

      assertThat(result).isEqualTo("No space")
    }
  }

  @Nested
  inner class GetRequestForPlacementWithdrawalDate {
    @Test
    fun `getRequestForPlacementWithdrawalDate returns null if request for placement event has not been withdrawn`() {
      val requestForPlacement = RequestForPlacementFactory().produce()

      every { domainEventRepository.findWithdrawnRequestForPlacement(requestForPlacement.id) } returns null

      val result = cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement)

      assertThat(result).isNull()
    }

    @Test
    fun `getRequestForPlacementWithdrawalDate returns withdrawalDate if request for placement event has been withdrawn`() {
      val requestForPlacement = RequestForPlacementFactory().produce()

      every { domainEventRepository.findWithdrawnRequestForPlacement(requestForPlacement.id) } returns DomainEventEntityFactory()
        .withOccurredAt(OffsetDateTime.now().minusDays(1))
        .produce()

      val result = cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement)

      assertThat(result).isEqualTo(LocalDate.now().minusDays(1))
    }
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
  inner class DefaultDurations {
    @Test
    fun `returns not found when application not found for applicationId`() {
      val applicationId = UUID.randomUUID()

      val sentenceType = SentenceTypeOption.entries.toTypedArray().random().value

      every { applicationService.getApplication(applicationId) } returns null

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(applicationId, mockk<ApType>(), sentenceType = sentenceType)

      assertThatCasResult(defaultDuration).isNotFound(
        "Application",
        expectedId = applicationId,
      )
    }

    @Test
    fun `returns not found error when tier is null`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .produce()

      every { applicationService.getApplication(application.id) } returns application
      every { tierService.getTier(application.crn) } returns null

      val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, mockk<ApType>(), SentenceTypeOption.entries.toTypedArray().random().value)

      assertThatCasResult(defaultDuration).isNotFound(
        "Tier associated with case CRN",
        expectedId = application.crn,
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
          .withDefaults()
          .produce()

        val sentenceType = SentenceTypeOption.entries.toTypedArray().random().value

        every { applicationService.getApplication(application.id) } returns application
        every { tierService.getTier(application.crn) } returns TierDtoFactory().withVersion(
          TierVersionDto.V2,
        ).produce()

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.valueOf(apType), sentenceType)

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(12).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }

      @Test
      fun `returns duration 26 weeks when apType is pipe`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withDefaults()
          .produce()

        val sentenceType = SentenceTypeOption.entries.toTypedArray().random().value

        every { applicationService.getApplication(application.id) } returns application
        every { tierService.getTier(application.crn) } returns TierDtoFactory().withVersion(
          TierVersionDto.V2,
        ).produce()

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.pipe, sentenceType)

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(26).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }

      @Test
      fun `returns duration 52 weeks when apType is esap`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withDefaults()
          .produce()

        val sentenceType = SentenceTypeOption.entries.toTypedArray().random().value

        every { applicationService.getApplication(application.id) } returns application
        every { tierService.getTier(application.crn) } returns TierDtoFactory().withVersion(
          TierVersionDto.V2,
        ).produce()

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(application.id, ApType.esap, sentenceType)

        assertThatCasResult(defaultDuration).isSuccess().with {
          assertThat(it.defaultDurationDays).isEqualTo(Period.ofWeeks(52).days)
          assertThat(it.maxDurationDays).isNull()
        }
      }
    }

    @Nested
    inner class V3 {

      @ParameterizedTest(name = "{0}")
      @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1RequestForPlacementServiceTest#durationCalculationExpectations")
      fun `calculate duration for tier v3 with all criteria combinations`(
        expectation: DurationCalculationExpectation,
      ) {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withDefaults()
          .withIsWomensApplication(expectation.isWomensApplication)
          .produce()

        every { applicationService.getApplication(application.id) } returns application

        every { tierService.getTier(application.crn) } returns TierDtoFactory()
          .withVersion(TierVersionDto.V3)
          .withTierScore(expectation.tier)
          .produce()

        val defaultDuration = cas1RequestForPlacementService.defaultDurations(
          applicationId = application.id,
          apType = expectation.apType,
          sentenceType = expectation.sentenceType.value,
          exceptionalApplication = expectation.exceptionStatus,
        )

        assertThatCasResult(defaultDuration).isSuccess().with {
          if (expectation.expectedDurationWeeks == null) {
            assertThat(it.defaultDurationDays).isNull()
          } else {
            assertThat(it.defaultDurationDays).isEqualTo(expectation.expectedDurationWeeks * 7)
          }
          assertThat(it.maxDurationDays).isNull()
        }

        if (expectation.expectedDurationWeeks == null) {
          verify {
            sentryService.captureErrorMessage(
              withArg { actual ->
                assertThat(actual).startsWith("Could not calculate duration for criteria")
              },
            )
          }
        } else {
          verify(exactly = 0) { sentryService.captureErrorMessage(any()) }
        }
      }
    }
  }

  companion object {
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    @SuppressWarnings("CyclomaticComplexMethod")
    @JvmStatic
    fun durationCalculationExpectations(): Stream<Arguments> {
      val path = "cas1/expected-duration-calculations.csv"

      val input = javaClass.classLoader.getResourceAsStream(path) ?: error("resource '$path' not found")

      val records = CSVFormat.DEFAULT
        .builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .build()
        .parse(InputStreamReader(input))

      val expectations = records.flatMap { record ->

        fun normaliseColValue(header: String) = record[header]?.trim()?.uppercase() ?: error("No $header defined")

        val apTypes = when (val apTypeRawValue = normaliseColValue("AP Type")) {
          "STANDARD AP" -> listOf(ApType.normal)
          "PIPE AP" -> listOf(ApType.pipe)
          "ESAP AP" -> listOf(ApType.esap)
          "RFAP AP" -> listOf(ApType.rfap)
          "MENTAL HEALTH AP" -> listOf(ApType.mhapElliottHouse, ApType.mhapStJosephs)
          "ANY" -> ApType.entries
          else -> error("Unknown AP Type $apTypeRawValue")
        }

        val placementPeriodResultRawValue = normaliseColValue("Placement Period Result")
        val expectedDurationWeeks = if (placementPeriodResultRawValue.endsWith("WEEKS")) {
          // MAX xyz Weeks
          if (placementPeriodResultRawValue.startsWith("MAX")) {
            placementPeriodResultRawValue.split(" ")[1].toInt()
            // xyz Weeks
          } else {
            placementPeriodResultRawValue.split(" ")[0].toInt()
          }
        } else if (placementPeriodResultRawValue == "CANNOT BE CALCULATED") {
          null
        } else {
          error("Unexpected value '$placementPeriodResultRawValue'")
        }

        val tierScore = when (val tierScoreRaw = normaliseColValue("Tier Score")) {
          "UNSUPERVISED" -> "NOT_SUPERVISED"
          else -> tierScoreRaw
        }

        val isWomensApplication = normaliseColValue("Gender") == "FEMALE"

        val sentenceTypes = when (val sentenceTypeRaw = normaliseColValue("Sentence type")) {
          "IPP" -> listOf(SentenceTypeOption.ipp)
          "NOT IPP" -> SentenceTypeOption.entries.filter { it != SentenceTypeOption.ipp }
          "ANY" -> SentenceTypeOption.entries.toList()
          else -> error("Unexpected value '$sentenceTypeRaw'")
        }

        val exceptionStatuses = when (val exceptionStatusRaw = normaliseColValue("Exception Status")) {
          "FALSE" -> listOf(false)
          "TRUE" -> listOf(true)
          "ANY" -> listOf(true, false)
          else -> error("Unexpected value '$exceptionStatusRaw'")
        }

        apTypes.flatMap { apType ->
          sentenceTypes.flatMap { sentenceType ->
            exceptionStatuses.map { exceptionStatus ->
              DurationCalculationExpectation(
                fromCsvRow = record.recordNumber + 1,
                apType = apType,
                tier = tierScore,
                isWomensApplication = isWomensApplication,
                sentenceType = sentenceType,
                exceptionStatus = exceptionStatus,
                expectedDurationWeeks = expectedDurationWeeks,
              )
            }
          }
        }
      }

      return expectations.map { Arguments.of(it) }.stream()
    }
  }
}
