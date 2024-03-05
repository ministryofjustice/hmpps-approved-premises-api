package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.ReportService
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ReportServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockTransitionalAccommodationReferralReportRowRepository =
    mockk<TransitionalAccommodationReferralReportRepository>()

  private val reportService = ReportService(
    mockOffenderService,
    mockUserService,
    mockTransitionalAccommodationReferralReportRowRepository,
    0,
    2,
  )

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report with required information`() {
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportData()
    val properties = TransitionalAccommodationReferralReportProperties(ServiceName.temporaryAccommodation, probationRegionId, 2024, 1)

    every { mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(any(), any(), any()) } returns listOf(testTransitionalAccommodationReferralReportData)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    reportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 31),
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report for 3 months`() {
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportData()
    val properties = TransitionalAccommodationReferralReportProperties(ServiceName.temporaryAccommodation, probationRegionId, 2024, 1)

    every { mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(any(), any(), any()) } returns listOf(testTransitionalAccommodationReferralReportData)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    val reportServiceWithThreeMonths = ReportService(
      mockOffenderService,
      mockUserService,
      mockTransitionalAccommodationReferralReportRowRepository,
      3,
      2,
    )

    reportServiceWithThreeMonths.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 4, 1),
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate empty report`() {
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(ServiceName.temporaryAccommodation, probationRegionId, 2024, 1)

    every { mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(any(), any(), any()) } returns emptyList()
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns emptyList()

    reportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 31),
        probationRegionId,
      )
    }
    verify(exactly = 1) { mockUserService.getUserForRequest() }
    verify(exactly = 0) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report and call offender service 2 times when crn search limit exceed`() {
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(ServiceName.temporaryAccommodation, probationRegionId, 2024, 1)

    every {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        any(),
        any(),
        any(),
      )
    } returns listOf(
      createDBReferralReportData("crn1"),
      createDBReferralReportData("crn2"),
      createDBReferralReportData("crn3"),
    )
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    reportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 31),
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 2) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report fail when offender service call fails`() {
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(ServiceName.temporaryAccommodation, probationRegionId, 2024, 1)

    every {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        any(),
        any(),
        any(),
      )
    } returns listOf(
      createDBReferralReportData("crn1"),
      createDBReferralReportData("crn2"),
      createDBReferralReportData("crn3"),
    )
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2"), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn3"), any()) } throws RuntimeException("some exception")

    Assertions.assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy {
        reportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())
      }

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 31),
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 2) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  private fun createDBReferralReportData(): TestTransitionalAccommodationReferralReportData {
    return createDBReferralReportData("crn")
  }
  private fun createDBReferralReportData(crn: String) = TestTransitionalAccommodationReferralReportData(
    UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
    LocalDate.now(), crn, LocalDate.now(), "riskOfSeriousHarm",
    sexOffender = false,
    needForAccessibleProperty = true,
    historyOfArsonOffence = false,
    dutyToReferMade = true,
    dateDutyToReferMade = LocalDate.now(),
    probationRegionName = "region",
    dutyToReferLocalAuthorityAreaName = "name",
    assessmentDecision = null,
    assessmentRejectionReason = null,
    assessmentSubmittedDate = LocalDate.now(),
    referralEligibleForCas3 = true,
    referralEligibilityReason = "reason",
    accommodationRequiredDate = Timestamp.valueOf(LocalDateTime.now()),
    prisonNameOnCreation = null,
    personReleaseDate = null,
    town = null,
    postCode = null,
  )

  @Suppress("LongParameterList")
  class TestTransitionalAccommodationReferralReportData(
    override val assessmentId: String,
    override val referralId: String,
    override val bookingId: String?,
    override val referralCreatedDate: LocalDate,
    override val crn: String,
    override val referralSubmittedDate: LocalDate?,
    override val riskOfSeriousHarm: String?,
    override val sexOffender: Boolean?,
    override val needForAccessibleProperty: Boolean?,
    override val historyOfArsonOffence: Boolean?,
    override val dutyToReferMade: Boolean?,
    override val dateDutyToReferMade: LocalDate?,
    override val probationRegionName: String,
    override val dutyToReferLocalAuthorityAreaName: String?,
    override val assessmentDecision: String?,
    override val assessmentRejectionReason: String?,
    override val assessmentSubmittedDate: LocalDate?,
    override val referralEligibleForCas3: Boolean?,
    override val referralEligibilityReason: String?,
    override val accommodationRequiredDate: Timestamp?,
    override val prisonNameOnCreation: String?,
    override val personReleaseDate: LocalDate?,
    override val town: String?,
    override val postCode: String?,
  ) : TransitionalAccommodationReferralReportData
}
