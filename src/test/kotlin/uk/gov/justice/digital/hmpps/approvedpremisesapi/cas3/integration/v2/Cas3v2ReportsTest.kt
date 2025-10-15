package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import com.opencsv.CSVReaderBuilder
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns.Remove
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.jetbrains.kotlinx.dataframe.size
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3InReview
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3ReadyToPlace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3Unallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BookingGapReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.FutureBookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toYesNo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.util.toBookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.listOf

class Cas3v2ReportsTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    mockFeatureFlagService.setFlag("cas3-reports-with-new-bedspace-model-tables-enabled", true)
  }

  @AfterEach
  fun afterEach() {
    mockFeatureFlagService.reset()
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report for all regions returns 403 Forbidden if user does not have all regions access`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report for a region returns 403 Forbidden if user cannot access the specified region`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02&probationRegionId=${UUID.randomUUID()}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get bookings report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`(
    reportType: Cas3ReportType,
  ) {
    givenAUser { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if dates provided is more than or equal to 3 months`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-04-01"
      val endDate = "2023-08-02"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("rangeTooLarge")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.endDate")
    }

    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-04-01"
      val endDate = "2023-07-01"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("rangeTooLarge")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.endDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if start date is later than end date`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-08-03"
      val endDate = "2023-08-02"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("afterEndDate")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.startDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if start date is the same as end date`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-08-02"
      val endDate = "2023-08-02"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("afterEndDate")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.startDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if end date is in the future`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val today = LocalDate.now()
      val startDate = "2023-08-02"
      val endDate = today.plusDays(1)
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("inFuture")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.endDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if mandatory dates are not provided`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Missing required query parameter startDate")
    }
  }

  @Test
  fun `Get report returns 400 when requested for invalid report type`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-08-01"
      val endDate = "2023-08-02"
      val actualBody = webTestClient.get()
        .uri("/cas3/reports/lostbed?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectBody()

      assertThat(actualBody.returnResult().status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  @Nested
  inner class GetReferralReport {

    @Test
    fun `Get CAS3 referral report OK response if user role is CAS3_ASSESSOR and requested regionId is allowed region`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referral?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Get CAS3 referral report returns OK response if user is CAS3_REPORTER and allow access to all region when no region is requested `() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { _, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Get CAS3 referral report returns OK response if user is CAS3_REPORTER the request region not matched to user region`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${UUID.randomUUID()}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @ParameterizedTest
    @EnumSource
    @NullSource
    fun `Get CAS3 referral report successfully with single matching referral in the report with different 'assessmentDecision'`(
      assessmentDecision: AssessmentDecision?,
    ) {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withProbationDeliveryUnit(user.probationDeliveryUnit)
            withArrivalDate(LocalDate.now().randomDateAfter(14))
            withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
            withCreatedAt(OffsetDateTime.now())
            withDutyToReferLocalAuthorityAreaName("London")
            withDutyToReferSubmissionDate(LocalDate.now())
            withHasHistoryOfArson(true)
            withIsConcerningArsonBehaviour(false)
            withIsDutyToReferSubmitted(true)
            withHasRegisteredSexOffender(true)
            withHasHistoryOfSexualOffence(false)
            withIsConcerningSexualBehaviour(null)
            withNeedsAccessibleProperty(true)
            withRiskRatings {
              withRoshRisks(
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
            }
            withPrisonNameAtReferral("HM Hounslow")
            withPersonReleaseDate(LocalDate.now())
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(assessmentDecision)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            assessmentDecision?.let { withSubmittedAt(OffsetDateTime.now()) }
            withRejectionRationale(if (REJECTED.name == assessmentDecision?.name) "some reason" else null)
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(1)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertCorrectReferralDetails(assessment, actual[0], "No")
            }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS3_ASSESSOR", "CAS3_REPORTER"])
    fun `Get CAS3 referral successfully with multiple referrals in the report and filter by start and endDate period`(
      userRole: UserRole,
    ) {
      givenAUser(roles = listOf(userRole)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val assessmentInReview = createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3InReview,
            LocalDate.parse("2024-01-01"),
          )
          val assessmentUnAllocated = createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3Unallocated,
            LocalDate.parse("2024-01-31"),
          )
          val assessmentReadyToPlace = createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3ReadyToPlace,
            LocalDate.parse("2024-01-15"),
          )
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3ReadyToPlace,
            LocalDate.parse("2024-02-15"),
          )
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3ReadyToPlace,
            LocalDate.parse("2023-12-15"),
          )

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-31&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { res ->
              val actual = DataFrame
                .readExcel(res.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(3)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertCorrectReferralDetails(
                assessmentInReview,
                actual.find { it.referralId == assessmentInReview.application.id.toString() }!!,
                "No",
              )
              assertCorrectReferralDetails(
                assessmentUnAllocated,
                actual.find { it.referralId == assessmentUnAllocated.application.id.toString() }!!,
                "No",
              )
              assertCorrectReferralDetails(
                assessmentReadyToPlace,
                actual.find { it.referralId == assessmentReadyToPlace.application.id.toString() }!!,
                "No",
              )
            }
        }
      }
    }

    @Test
    fun `Get CAS3 referral successfully with referral has been offered with booking`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }

          val (premises, bedspace, application) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC),
            LocalDate.now().randomDateAfter(30),
            null,
          )

          cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2024, 1, 1))
            withDepartureDate(LocalDate.of(2024, 1, 1))
            withApplication(application)
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(1)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertThat(actual[0].bookingOffered).isEqualTo("Yes")
              assertThat(actual[0].town).isEqualTo(premises.town)
              assertThat(actual[0].postCode).isEqualTo(premises.postcode)
            }
        }
      }
    }

    @Test
    fun `Get CAS3 referral report successfully with multiple assessment for single referral`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }

          val (premises, bedspace, application) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC),
            LocalDate.now().randomDateAfter(30),
            null,
          )

          temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withSubmittedAt(OffsetDateTime.now())
          }

          cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2024, 1, 1))
            withDepartureDate(LocalDate.of(2024, 1, 1))
            withApplication(application)
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(2)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertThat(actual[0].bookingOffered).isEqualTo("Yes")
            }
        }
      }
    }

    @Test
    fun `Get empty CAS3 referral report when no matching application found in DB`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3InReview,
            LocalDate.parse("2024-01-01"),
          )
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3Unallocated,
            LocalDate.parse("2024-01-31"),
          )
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3ReadyToPlace,
            LocalDate.parse("2024-01-15"),
          )
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3ReadyToPlace,
            LocalDate.parse("2024-02-15"),
          )
          createTemporaryAccommodationAssessmentForStatus(
            user,
            offenderDetails,
            cas3ReadyToPlace,
            LocalDate.parse("2023-12-15"),
          )

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-03-01&endDate=2024-03-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual).isEmpty()
            }
        }
      }
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "21b8569c-ef2e-4059-8676-323098d16aa5",
        "11506230-49a8-48b5-bdf5-20f51324e8a5",
        "a1c7d402-77b5-4335-a67b-eba6a71c70bf",
        "88c3b8d5-77c8-4c52-84f0-ec9073e4df50",
        "90e9d919-9a39-45cd-b405-7039b5640668",
        "155ee6dc-ac2a-40d2-a350-90b63fb34a06",
        "85799bf8-8b64-4903-9ab8-b08a77f1a9d3",
      ],
    )
    fun `Get CAS3 referral report successfully with single matching referral in the report with different Referral Rejection Reasons`(
      referralRejectionReasonId: UUID,
    ) {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withProbationDeliveryUnit(user.probationDeliveryUnit)
            withArrivalDate(LocalDate.now().randomDateAfter(14))
            withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
            withCreatedAt(OffsetDateTime.now())
            withDutyToReferLocalAuthorityAreaName("London")
            withDutyToReferSubmissionDate(LocalDate.now())
            withHasHistoryOfArson(false)
            withIsConcerningArsonBehaviour(true)
            withIsDutyToReferSubmitted(true)
            withHasRegisteredSexOffender(null)
            withHasHistoryOfSexualOffence(false)
            withIsConcerningSexualBehaviour(true)
            withNeedsAccessibleProperty(true)
            withRiskRatings {
              withRoshRisks(
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
            }
            withPrisonNameAtReferral("HM Hounslow")
            withPersonReleaseDate(LocalDate.now())
          }

          val referralRejectionReason = referralRejectionReasonRepository.findByIdOrNull(referralRejectionReasonId)

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            REJECTED.let { withSubmittedAt(OffsetDateTime.now()) }
            withRejectionRationale("some reason")
            withReferralRejectionReason(referralRejectionReason!!)
          }

          if (referralRejectionReason?.id == UUID.fromString("85799bf8-8b64-4903-9ab8-b08a77f1a9d3")) {
            assessment.referralRejectionReasonDetail = randomStringLowerCase(100)
            temporaryAccommodationAssessmentRepository.save(assessment)
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(1)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertCorrectReferralDetails(assessment, actual[0], "Yes")
            }

          assertThat(referralRejectionReason).isNotNull()
        }
      }
    }

    @Test
    fun `Get CAS3 referral report successfully with single matching referral in the report with type of prison release`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val prisonReleaseTypes = "Standard recall,CRD licence,ECSL"
          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withProbationDeliveryUnit(user.probationDeliveryUnit)
            withArrivalDate(LocalDate.now().randomDateAfter(14))
            withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
            withCreatedAt(OffsetDateTime.now())
            withDutyToReferLocalAuthorityAreaName("London")
            withDutyToReferSubmissionDate(LocalDate.now())
            withHasHistoryOfArson(false)
            withIsConcerningArsonBehaviour(true)
            withIsDutyToReferSubmitted(true)
            withHasRegisteredSexOffender(null)
            withHasHistoryOfSexualOffence(false)
            withIsConcerningSexualBehaviour(true)
            withNeedsAccessibleProperty(true)
            withPrisonReleaseTypes(prisonReleaseTypes)
            withRiskRatings {
              withRoshRisks(
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
            }
            withPrisonNameAtReferral("HM Hounslow")
            withPersonReleaseDate(LocalDate.now())
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            REJECTED.let { withSubmittedAt(OffsetDateTime.now()) }
            withRejectionRationale("some reason")
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(1)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertCorrectReferralDetails(assessment, actual[0], "No")
              assertThat(prisonReleaseTypes).isEqualTo(actual[0].prisonReleaseType)
            }
        }
      }
    }

    @Test
    fun `CAS3 referral report shows updatedReleaseDate and updatedAccommodationRequiredFrom dates in report when they have been updated`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }

          val prisonReleaseTypes = "Standard recall,CRD licence,ECSL"
          val application =
            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(user)
              withProbationRegion(user.probationRegion)
              withArrivalDate(LocalDate.now().plusDays(1))
              withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
              withCreatedAt(OffsetDateTime.now())
              withDutyToReferLocalAuthorityAreaName("London")
              withDutyToReferSubmissionDate(LocalDate.now())
              withHasHistoryOfArson(false)
              withIsConcerningArsonBehaviour(true)
              withIsDutyToReferSubmitted(true)
              withHasRegisteredSexOffender(null)
              withHasHistoryOfSexualOffence(false)
              withIsConcerningSexualBehaviour(true)
              withNeedsAccessibleProperty(true)
              withPrisonReleaseTypes(prisonReleaseTypes)
              withRiskRatings {
                withRoshRisks(
                  RiskWithStatus(
                    value =
                    RoshRisks(
                      overallRisk = "High",
                      riskToChildren = "Medium",
                      riskToPublic = "Low",
                      riskToKnownAdult = "High",
                      riskToStaff = "High",
                      lastUpdated = null,
                    ),
                  ),
                )
              }
              withPrisonNameAtReferral("HM Hounslow")
              withPersonReleaseDate(LocalDate.now())
              withProbationDeliveryUnit(probationDeliveryUnit)
            }

          val assessment =
            temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
              withApplication(application)
              withDecision(REJECTED)
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              REJECTED.let { withSubmittedAt(OffsetDateTime.now()) }
              withRejectionRationale("some reason")
              withReleaseDate(LocalDate.now().plusDays(9))
              withAccommodationRequiredFromDate(LocalDate.now().plusDays(10))
            }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient
            .get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val reportRow =
                DataFrame
                  .readExcel(it.responseBody!!.inputStream())
                  .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                  .toList()[0]

              assertThat(reportRow.releaseDate).isEqualTo(application.personReleaseDate)
              assertThat(reportRow.accommodationRequiredDate).isEqualTo(application.arrivalDate!!.toLocalDate())
              assertThat(reportRow.updatedReleaseDate).isEqualTo(assessment.releaseDate)
              assertThat(reportRow.updatedAccommodationRequiredFromDate).isEqualTo(assessment.accommodationRequiredFromDate)
            }
        }
      }
    }

    @Test
    fun `Get CAS3 referral report successfully when offender gender identity is Prefer to self-describe`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            OffenderDetailsSummaryFactory()
              .withGenderIdentity("Prefer to self-describe")
              .withSelfDescribedGenderIdentity(randomStringLowerCase(10))
              .produce()
          },
        ) { offenderDetails, _ ->

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }

          val (premises, bedspace, application) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC),
            LocalDate.now().randomDateAfter(30),
            null,
          )

          temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withSubmittedAt(OffsetDateTime.now())
          }

          cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2024, 1, 1))
            withDepartureDate(LocalDate.of(2024, 1, 1))
            withApplication(application)
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referral?startDate=2024-01-01&endDate=2024-01-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
                .toList()

              assertThat(actual.size).isEqualTo(2)
              assertCorrectPersonDetail(caseSummary, actual[0])
              assertThat(actual[0].bookingOffered).isEqualTo("Yes")
            }
        }
      }
    }
  }

  @Nested
  inner class GetBookingReport {

    @Test
    fun `Get bookings report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ).third,
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with latest departure and arrivals when booking has updated with multiple departures and arrivals`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ).third,
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }

          bookings[2].let {
            val firstArrivalUpdate = cas3ArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now().randomDateBefore(14))
            }
            val secondArrivalUpdate = cas3ArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now())
            }

            it.arrivals = listOf(firstArrivalUpdate, secondArrivalUpdate).toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()

            val firstDepartureUpdate = cas3DepartureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now().randomDateTimeBefore(14))
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            val secondDepartureUpdate = cas3DepartureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now())
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            it.departures = listOf(firstDepartureUpdate, secondDepartureUpdate).toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK for CAS3_REPORTER`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ).third,
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK for CAS3_REPORTER for all region`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ).third,
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns 403 Forbidden for CAS3_REPORTER with service-name as approved-premises`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->

        webTestClient.get()
          .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report returns OK with only Bookings with at least one day in month when year and month are specified`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = givenACas3Premises(
            user.probationRegion,
            status = Cas3PremisesStatus.online,
          )
          val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(startDate.minusDays(100))
            withCreatedDate(startDate.minusDays(100))
            withEndDate(null)
          }

          val shouldNotBeIncludedBookings = mutableListOf<Cas3BookingEntity>()
          val shouldBeIncludedBookings = mutableListOf<Cas3BookingEntity>()

          // Straddling start of month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 29))
            withDepartureDate(LocalDate.of(2023, 4, 1))
          }

          // Straddling end of month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 2))
            withDepartureDate(LocalDate.of(2023, 4, 3))
          }

          // Entirely within month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 30))
            withDepartureDate(LocalDate.of(2023, 5, 15))
          }

          // Encompassing month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 5, 28))
          }

          // Before month
          shouldNotBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 3, 30))
          }

          // After month
          shouldNotBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 5, 1))
            withDepartureDate(LocalDate.of(2023, 5, 3))
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              shouldBeIncludedBookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with only bookings from the specified probation region`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ).third,
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          // Unexpected bookings
          val unexpectedPremises = givenACas3Premises(
            status = Cas3PremisesStatus.online,
          )
          val unexpectedBedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(unexpectedPremises)
            withStartDate(startDate.minusDays(100))
            withCreatedDate(startDate.minusDays(100))
            withEndDate(null)
          }
          cas3BookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(unexpectedPremises)
            withBedspace(unexpectedBedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with correct body and correct duty to refer local authority area name`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = givenACas3Premises(
            user.probationRegion,
            status = Cas3PremisesStatus.online,
          )
          val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(startDate.minusDays(100))
            withCreatedDate(startDate.minusDays(100))
            withEndDate(null)
          }

          val accommodationApplication =
            createTemporaryAccommodationApplication(offenderDetails, user)

          val bookings = cas3BookingEntityFactory.produceAndPersistMultiple(1) {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
            withApplication(accommodationApplication)
          }
          bookings[0].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with only bookings from the specified service`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ).third,
            )
          }

          bookings[1].let { it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val unexpectedPremises = givenAnApprovedPremises()

          // Unexpected bookings
          bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(unexpectedPremises)
            withServiceName(ServiceName.approvedPremises)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    private fun createTemporaryAccommodationApplication(
      offenderDetails: OffenderDetailSummary,
      user: UserEntity,
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withDutyToReferLocalAuthorityAreaName("London")
      withSubmittedAt(OffsetDateTime.now())
    }
  }

  @Nested
  inner class GetBedspaceUsageReport {

    @Test
    fun `Get bed usage report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val arrivalDate = LocalDate.parse("2023-04-05")
          val (premises, bedspace, _) = setupPremisesWIthABedspaceAndABooking(
            crn = offenderDetails.otherIds.crn,
            user,
            startDate = arrivalDate.minusDays(10),
            arrivalDate,
            departureDate = LocalDate.parse("2023-04-15"),
          )

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val expectedReportRows = listOf(
            BedUsageReportRow(
              probationRegion = user.probationRegion.name,
              pdu = premises.probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = bedspace.reference,
              crn = offenderDetails.otherIds.crn,
              type = BedUsageType.Booking,
              startDate = LocalDate.parse("2023-04-05"),
              endDate = LocalDate.parse("2023-04-15"),
              durationOfBookingDays = 10,
              bookingStatus = BookingStatus.provisional,
              voidCategory = null,
              voidNotes = null,
              costCentre = null,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = bedspace.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedUsage?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUsageReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }

  @Nested
  inner class GetFutureBookingsReport {
    @Test
    @SuppressWarnings("LongMethod")
    fun `Get future bookings report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.now().minusDays(30)
          val reportEndDate = LocalDate.now()

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }

          // offender with accommodation required date within report dates
          val premisesOneAccommodationRequiredDate = reportStartDate.plusDays(7)
          val (premisesOne, bedspaceOne, applicationOne) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesOneAccommodationRequiredDate,
            null,
          )

          val premisesTwoAccommodationRequiredDate = reportStartDate.plusDays(21)
          val (premisesTwo, bedspaceTwo, applicationTwo) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesTwoAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date before report start date
          val premisesThreeAccommodationRequiredDate = reportStartDate.minusDays(60)
          val (premisesThree, bedspaceThree, applicationThree) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesThreeAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date out of report dates range
          val premisesFourAccommodationRequiredDate = reportEndDate.plusDays(200)
          val (premisesFour, bedspaceFour, applicationFour) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesFourAccommodationRequiredDate,
            null,
          )

          // offender with updated accommodation required date within report dates range
          val premisesFiveAccommodationRequiredDate = reportStartDate.minusDays(50)
          val premisesFiveUpdatedAccommodationRequiredDate = reportStartDate.plusDays(23)
          val (premisesFive, bedspaceFive, applicationFive) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesFiveAccommodationRequiredDate,
            premisesFiveUpdatedAccommodationRequiredDate,
          )

          val premisesSixAccommodationRequiredDate = reportStartDate.plusDays(210)
          val premisesSixUpdatedAccommodationRequiredDate = reportEndDate.plusDays(9)
          val (premisesSix, bedspaceSix, applicationSix) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesSixAccommodationRequiredDate,
            premisesSixUpdatedAccommodationRequiredDate,
          )

          // offender with updated accommodation required date out of report dates range
          val premisesSevenAccommodationRequiredDate = reportStartDate.minusDays(6)
          val (premisesSeven, bedspaceSeven, applicationSeven) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            reportEndDate.plusDays(3),
            premisesSevenAccommodationRequiredDate,
          )

          // provisional booking
          val bookingOne = createBooking(
            applicationOne,
            premisesOne,
            bedspaceOne,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(6),
            reportEndDate.plusDays(30),
          )

          val bookingTwo = createBooking(
            applicationFive,
            premisesFive,
            bedspaceFive,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(3),
            reportStartDate.plusDays(18),
          )

          val bookingThree = createBooking(
            applicationSix,
            premisesSix,
            bedspaceSix,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(13),
            reportStartDate.plusDays(56),
          )

          // old booking
          createBooking(
            applicationThree,
            premisesThree,
            bedspaceThree,
            offenderDetails.otherIds.crn,
            reportStartDate.minusDays(90),
            reportStartDate.minusDays(15),
          )

          // confirmed booking
          val bookingFour = createBooking(
            applicationTwo,
            premisesTwo,
            bedspaceTwo,
            offenderDetails.otherIds.crn,
            reportStartDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(20),
          )
          bookingFour.let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // cancelled booking
          createBooking(
            applicationOne,
            premisesOne,
            bedspaceOne,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(8),
            reportStartDate.plusDays(30),
          ).let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }

          // future confirmed booking
          val bookingFive = createBooking(
            applicationFive,
            premisesFive,
            bedspaceFive,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(40),
          )
          bookingFive.let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // future booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            bedspaceFour,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(70),
            reportEndDate.randomDateAfter(90),
          )

          // future confirmed booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            bedspaceFour,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(90),
            reportEndDate.plusDays(120),
          ).let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          createBooking(
            applicationSeven,
            premisesSeven,
            bedspaceSeven,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(70),
            reportEndDate.plusDays(85),
          ).let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/futureBookings?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<FutureBookingsReportRow>(Remove)

              assertThat(actual.size().nrow).isEqualTo(5)

              assertRow(
                actual[0],
                bookingTwo.id,
                offenderDetails,
                applicationFive,
                premisesFive,
                bookingTwo.arrivalDate,
                premisesFiveAccommodationRequiredDate,
                premisesFiveUpdatedAccommodationRequiredDate,
                "Provisional",
              )
              assertRow(
                actual[1],
                bookingFive.id,
                offenderDetails,
                applicationFive,
                premisesFive,
                bookingFive.arrivalDate,
                premisesFiveAccommodationRequiredDate,
                premisesFiveUpdatedAccommodationRequiredDate,
                "Confirmed",
              )
              assertRow(
                actual[2],
                bookingOne.id,
                offenderDetails,
                applicationOne,
                premisesOne,
                bookingOne.arrivalDate,
                premisesOneAccommodationRequiredDate,
                null,
                "Provisional",
              )
              assertRow(
                actual[3],
                bookingFour.id,
                offenderDetails,
                applicationTwo,
                premisesTwo,
                bookingFour.arrivalDate,
                premisesTwoAccommodationRequiredDate,
                null,
                "Confirmed",
              )
              assertRow(
                actual[4],
                bookingThree.id,
                offenderDetails,
                applicationSix,
                premisesSix,
                bookingThree.arrivalDate,
                premisesSixAccommodationRequiredDate,
                premisesSixUpdatedAccommodationRequiredDate,
                "Provisional",
              )
            }
        }
      }
    }

    @Test
    @SuppressWarnings("LongMethod")
    fun `Get future bookings report in Csv format returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.now().minusDays(30)
          val reportEndDate = LocalDate.now()

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }

          // offender with accommodation required date within report dates
          val premisesOneAccommodationRequiredDate = reportStartDate.plusDays(7)
          val (premisesOne, bedspaceOne, applicationOne) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesOneAccommodationRequiredDate,
            null,
          )

          val premisesTwoAccommodationRequiredDate = reportStartDate.plusDays(21)
          val (premisesTwo, bedspaceTwo, applicationTwo) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesTwoAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date before report start date
          val premisesThreeAccommodationRequiredDate = reportStartDate.minusDays(60)
          val (premisesThree, bedspaceThree, applicationThree) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesThreeAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date out of report dates range
          val premisesFourAccommodationRequiredDate = reportEndDate.plusDays(200)
          val (premisesFour, bedspaceFour, applicationFour) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesFourAccommodationRequiredDate,
            null,
          )

          // offender with updated accommodation required date within report dates range
          val premisesFiveAccommodationRequiredDate = reportStartDate.minusDays(50)
          val premisesFiveUpdatedAccommodationRequiredDate = reportStartDate.plusDays(23)
          val (premisesFive, bedspaceFive, applicationFive) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesFiveAccommodationRequiredDate,
            premisesFiveUpdatedAccommodationRequiredDate,
          )

          val premisesSixAccommodationRequiredDate = reportStartDate.plusDays(210)
          val premisesSixUpdatedAccommodationRequiredDate = reportEndDate.plusDays(9)
          val (premisesSix, bedspaceSix, applicationSix) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesSixAccommodationRequiredDate,
            premisesSixUpdatedAccommodationRequiredDate,
          )

          // offender with updated accommodation required date out of report dates range
          val premisesSevenAccommodationRequiredDate = reportStartDate.minusDays(6)
          val (premisesSeven, bedspaceSeven, applicationSeven) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            reportEndDate.plusDays(3),
            premisesSevenAccommodationRequiredDate,
          )

          // provisional booking
          val bookingOne = createBooking(
            applicationOne,
            premisesOne,
            bedspaceOne,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(6),
            reportEndDate.plusDays(30),
          )

          val bookingTwo = createBooking(
            applicationFive,
            premisesFive,
            bedspaceFive,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(3),
            reportStartDate.plusDays(18),
          )

          val bookingThree = createBooking(
            applicationSix,
            premisesSix,
            bedspaceSix,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(13),
            reportStartDate.plusDays(56),
          )

          // old booking
          createBooking(
            applicationThree,
            premisesThree,
            bedspaceThree,
            offenderDetails.otherIds.crn,
            reportStartDate.minusDays(90),
            reportStartDate.minusDays(15),
          )

          // confirmed booking
          val bookingFour = createBooking(
            applicationTwo,
            premisesTwo,
            bedspaceTwo,
            offenderDetails.otherIds.crn,
            reportStartDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(20),
          )
          bookingFour.let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // cancelled booking
          createBooking(
            applicationOne,
            premisesOne,
            bedspaceOne,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(8),
            reportStartDate.plusDays(30),
          ).let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }

          // future confirmed booking
          val bookingFive = createBooking(
            applicationFive,
            premisesFive,
            bedspaceFive,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(40),
          )
          bookingFive.let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // future booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            bedspaceFour,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(70),
            reportEndDate.randomDateAfter(90),
          )

          // future confirmed booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            bedspaceFour,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(90),
            reportEndDate.plusDays(120),
          ).let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          createBooking(
            applicationSeven,
            premisesSeven,
            bedspaceSeven,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(70),
            reportEndDate.plusDays(85),
          ).let {
            it.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/futureBookingsCsv?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { response ->
              val completeCsvString = response.responseBody!!.inputStream().bufferedReader().use { it.readText() }

              val csvReader = CSVReaderBuilder(StringReader(completeCsvString)).build()
              val headers = csvReader.readNext().toList()

              assertReportHeader(headers)

              val actual = DataFrame
                .readCSV(completeCsvString.byteInputStream())
                .convertTo<FutureBookingsReportRow>()
                .toList()

              assertThat(actual.size).isEqualTo(5)

              assertReportRow(
                actual[0],
                bookingTwo.id,
                offenderDetails,
                applicationFive,
                premisesFive,
                bookingTwo.arrivalDate,
                premisesFiveAccommodationRequiredDate,
                premisesFiveUpdatedAccommodationRequiredDate,
                "Provisional",
              )

              assertReportRow(
                actual[1],
                bookingFive.id,
                offenderDetails,
                applicationFive,
                premisesFive,
                bookingFive.arrivalDate,
                premisesFiveAccommodationRequiredDate,
                premisesFiveUpdatedAccommodationRequiredDate,
                "Confirmed",
              )

              assertReportRow(
                actual[2],
                bookingOne.id,
                offenderDetails,
                applicationOne,
                premisesOne,
                bookingOne.arrivalDate,
                premisesOneAccommodationRequiredDate,
                null,
                "Provisional",
              )

              assertReportRow(
                actual[3],
                bookingFour.id,
                offenderDetails,
                applicationTwo,
                premisesTwo,
                bookingFour.arrivalDate,
                premisesTwoAccommodationRequiredDate,
                null,
                "Confirmed",
              )

              assertReportRow(
                actual[4],
                bookingThree.id,
                offenderDetails,
                applicationSix,
                premisesSix,
                bookingThree.arrivalDate,
                premisesSixAccommodationRequiredDate,
                premisesSixUpdatedAccommodationRequiredDate,
                "Provisional",
              )
            }
        }
      }
    }

    @Suppress("LongParameterList")
    fun assertRow(
      row: DataRow<FutureBookingsReportRow>,
      bookingId: UUID,
      offenderDetails: OffenderDetailSummary,
      application: TemporaryAccommodationApplicationEntity,
      premises: Cas3PremisesEntity,
      bookingStartDate: LocalDate,
      accommodationRequiredDate: LocalDate,
      updateAccommodationRequiredDate: LocalDate?,
      bookingStatus: String,
    ) {
      val expectedPersonName = if (offenderDetails.middleNames.isNullOrEmpty()) {
        "${offenderDetails.firstName} ${offenderDetails.surname}"
      } else {
        (listOf(offenderDetails.firstName) + offenderDetails.middleNames + offenderDetails.surname).joinToString(
          " ",
        )
      }

      assertThat(row["bookingId"]).isEqualTo(bookingId.toString())
      assertThat(row["referralId"]).isEqualTo(application.id.toString())
      assertThat(row["referralDate"]).isEqualTo(application.submittedAt?.toLocalDate())
      assertThat(row["personName"]).isEqualTo(expectedPersonName)
      assertThat(row["gender"]).isEqualTo(offenderDetails.gender)
      assertThat(row["ethnicity"]).isEqualTo(offenderDetails.offenderProfile.ethnicity)
      assertThat(row["dateOfBirth"]).isEqualTo(offenderDetails.dateOfBirth)
      assertThat(row["riskOfSeriousHarm"]).isEqualTo("High")
      assertThat(row["registeredSexOffender"]).isEqualTo(application.isRegisteredSexOffender.toYesNo())
      assertThat(row["historyOfSexualOffence"]).isEqualTo(application.isHistoryOfSexualOffence.toYesNo())
      assertThat(row["concerningSexualBehaviour"]).isEqualTo(application.isConcerningSexualBehaviour.toYesNo())
      assertThat(row["dutyToReferMade"]).isEqualTo(application.isDutyToReferSubmitted.toYesNo())
      assertThat(row["dateDutyToReferMade"]).isEqualTo(application.dutyToReferSubmissionDate)
      assertThat(row["dutyToReferLocalAuthorityAreaName"]).isEqualTo(application.dutyToReferLocalAuthorityAreaName)
      assertThat(row["probationRegion"]).isEqualTo(application.probationRegion.name)
      assertThat(row["pdu"]).isEqualTo(application.probationDeliveryUnit?.name)
      assertThat(row["localAuthority"]).isEqualTo(premises.localAuthorityArea?.name)
      assertThat(row["addressLine1"]).isEqualTo(premises.addressLine1)
      assertThat(row["postCode"]).isEqualTo(premises.postcode)
      assertThat(row["crn"]).isEqualTo(application.crn)
      assertThat(row["sourceOfReferral"]).isEqualTo(application.eligibilityReason)
      assertThat(row["prisonAtReferral"]).isEqualTo(application.prisonNameOnCreation)
      assertThat(row["startDate"]).isEqualTo(bookingStartDate)
      assertThat(row["accommodationRequiredDate"]).isEqualTo(accommodationRequiredDate)
      assertThat(row["updatedAccommodationRequiredDate"]).isEqualTo(updateAccommodationRequiredDate)
      assertThat(row["bookingStatus"]).isEqualTo(bookingStatus)
    }

    fun assertReportHeader(headers: List<String>) {
      assertThat(headers).contains("bookingId")
      assertThat(headers).contains("referralId")
      assertThat(headers).contains("referralDate")
      assertThat(headers).contains("personName")
      assertThat(headers).contains("gender")
      assertThat(headers).contains("ethnicity")
      assertThat(headers).contains("dateOfBirth")
      assertThat(headers).contains("riskOfSeriousHarm")
      assertThat(headers).contains("registeredSexOffender")
      assertThat(headers).contains("historyOfSexualOffence")
      assertThat(headers).contains("concerningSexualBehaviour")
      assertThat(headers).contains("dutyToReferMade")
      assertThat(headers).contains("dateDutyToReferMade")
      assertThat(headers).contains("dutyToReferLocalAuthorityAreaName")
      assertThat(headers).contains("probationRegion")
      assertThat(headers).contains("pdu")
      assertThat(headers).contains("localAuthority")
      assertThat(headers).contains("addressLine1")
      assertThat(headers).contains("postCode")
      assertThat(headers).contains("crn")
      assertThat(headers).contains("sourceOfReferral")
      assertThat(headers).contains("prisonAtReferral")
      assertThat(headers).contains("startDate")
      assertThat(headers).contains("accommodationRequiredDate")
      assertThat(headers).contains("updatedAccommodationRequiredDate")
      assertThat(headers).contains("bookingStatus")
    }

    @Suppress("LongParameterList")
    fun assertReportRow(
      row: FutureBookingsReportRow,
      bookingId: UUID,
      offenderDetails: OffenderDetailSummary,
      application: TemporaryAccommodationApplicationEntity,
      premises: Cas3PremisesEntity,
      bookingStartDate: LocalDate,
      accommodationRequiredDate: LocalDate,
      updateAccommodationRequiredDate: LocalDate?,
      bookingStatus: String,
    ) {
      val expectedPersonName = if (offenderDetails.middleNames.isNullOrEmpty()) {
        "${offenderDetails.firstName} ${offenderDetails.surname}"
      } else {
        (listOf(offenderDetails.firstName) + offenderDetails.middleNames + offenderDetails.surname).joinToString(
          " ",
        )
      }

      assertThat(row.bookingId).isEqualTo(bookingId.toString())
      assertThat(row.referralId).isEqualTo(application.id.toString())
      assertThat(row.referralDate).isEqualTo(application.submittedAt?.toLocalDate())
      assertThat(row.personName).isEqualTo(expectedPersonName)
      assertThat(row.gender).isEqualTo(offenderDetails.gender)
      assertThat(row.ethnicity).isEqualTo(offenderDetails.offenderProfile.ethnicity)
      assertThat(row.dateOfBirth).isEqualTo(offenderDetails.dateOfBirth)
      assertThat(row.riskOfSeriousHarm).isEqualTo("High")
      assertThat(row.registeredSexOffender).isEqualTo(application.isRegisteredSexOffender.toString())
      assertThat(row.historyOfSexualOffence).isEqualTo(application.isHistoryOfSexualOffence.toString())
      assertThat(row.concerningSexualBehaviour).isEqualTo(application.isConcerningSexualBehaviour.toString())
      assertThat(row.dutyToReferMade).isEqualTo(application.isDutyToReferSubmitted.toString())
      assertThat(row.dateDutyToReferMade).isEqualTo(application.dutyToReferSubmissionDate)
      assertThat(row.dutyToReferLocalAuthorityAreaName).isEqualTo(application.dutyToReferLocalAuthorityAreaName)
      assertThat(row.probationRegion).isEqualTo(application.probationRegion.name)
      assertThat(row.pdu).isEqualTo(application.probationDeliveryUnit?.name)
      assertThat(row.localAuthority).isEqualTo(premises.localAuthorityArea?.name)
      assertThat(row.addressLine1).isEqualTo(premises.addressLine1)
      assertThat(row.postCode).isEqualTo(premises.postcode)
      assertThat(row.crn).isEqualTo(application.crn)
      assertThat(row.sourceOfReferral).isEqualTo(application.eligibilityReason)
      assertThat(row.prisonAtReferral).isEqualTo(application.prisonNameOnCreation)
      assertThat(row.startDate).isEqualTo(bookingStartDate.toString())
      assertThat(row.accommodationRequiredDate).isEqualTo(accommodationRequiredDate.toString())
      assertThat(row.updatedAccommodationRequiredDate).isEqualTo(updateAccommodationRequiredDate)
      assertThat(row.bookingStatus).isEqualTo(bookingStatus)
    }

    @SuppressWarnings("LongParameterList")
    private fun createBooking(
      application: TemporaryAccommodationApplicationEntity,
      premises: Cas3PremisesEntity,
      bedspace: Cas3BedspacesEntity,
      crn: String,
      arrivalDate: LocalDate,
      departureDate: LocalDate,
    ): Cas3BookingEntity = cas3BookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPremises(premises)
      withBedspace(bedspace)
      withServiceName(ServiceName.temporaryAccommodation)
      withCrn(crn)
      withArrivalDate(arrivalDate)
      withDepartureDate(departureDate)
    }
  }

  @Nested
  inner class GetBookingGapReport {

    @Test
    fun `Get booking gap report for single booking within report period returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 1. Single booking within report period
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-05"),
            LocalDate.parse("2024-04-12"),
          )

          val expectedReportRows = listOf(
            // 1. Single booking within report period
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-04]",
              4,
              null,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-13,2024-04-30]",
              18,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for multiple bookings for the same bedspace returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 2. Multiple bookings for the same bedspace
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-02"),
            LocalDate.parse("2024-04-06"),
          )
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-10"),
            LocalDate.parse("2024-04-15"),
          )

          // 3. Booking longer than report period
          val bed2 = createBedspace(premises, "bed2", bedStart, bedEnd)
          createBooking(
            premises,
            bed2,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-03-25"),
            LocalDate.parse("2024-05-05"),
          )

          val expectedReportRows = listOf(
            // 2. Multiple bookings for the same bedspace
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-01]",
              1,
              null,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-07,2024-04-09]",
              3,
              0,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-16,2024-04-30]",
              15,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bedspace start date after report start date returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 4. Bedspace start date after report start date
          val bed1 = createBedspace(premises, "bed1", LocalDate.parse("2024-04-10"), bedEnd)
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-12"),
            LocalDate.parse("2024-04-20"),
          )

          val expectedReportRows = listOf(
            // 4. Bedspace start date after report start date
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-10,2024-04-11]",
              2,
              null,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-21,2024-04-30]",
              10,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bedspace end date before report end date returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 5. Bedspace end date before report end date
          val bed1 = createBedspace(premises, "bed1", bedStart, LocalDate.parse("2024-04-20"))
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-05"),
            LocalDate.parse("2024-04-15"),
          )

          val expectedReportRows = listOf(
            // 5. Bedspace end date before report end date
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-04]",
              4,
              null,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-16,2024-04-20]",
              5,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bed with both bookings and voids returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 6. Bed with both bookings and voids
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-02"),
            LocalDate.parse("2024-04-06"),
          )
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bed1)
            withStartDate(LocalDate.parse("2024-04-08"))
            withEndDate(LocalDate.parse("2024-04-12"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val expectedReportRows = listOf(
            // 6. Bed with both bookings and voids
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-01]",
              1,
              null,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-07,2024-04-07]",
              1,
              0,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,

              "[2024-04-13,2024-04-30]",
              18,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bed with only voids & no bookings returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 7. Bed with only voids (no bookings)
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bed1)
            withStartDate(LocalDate.parse("2024-04-01"))
            withEndDate(LocalDate.parse("2024-04-10"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val expectedReportRows = listOf(
            // 7. Bed with only voids (no bookings)
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-11,2024-04-30]",
              20,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bed with cancelled bookings returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 8. Bed with cancelled bookings
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)

          val cancelledBooking = createBooking(premises, bed1, offenderDetails.otherIds.crn, LocalDate.parse("2024-04-05"), LocalDate.parse("2024-04-12"))
          val cancellationReason = cancellationReasonEntityFactory.produceAndPersist { withServiceScope("temporary-accommodation") }
          cas3CancellationEntityFactory.produceAndPersist {
            withBooking(cancelledBooking)
            withYieldedReason { cancellationReason }
          }

          val expectedReportRows = listOf(
            // 8. Bed with cancelled bookings
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-30]",
              30,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bed with gaps at the start and end returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 9. Gaps at the start or end of bed’s active period
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-10"),
            LocalDate.parse("2024-04-15"),
          )

          val expectedReportRows = listOf(
            // 9. Gaps at the start or end of bed’s active period
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-09]",
              9,
              null,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-16,2024-04-30]",
              15,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for adjacent bookings wtih no gap returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 11. Adjacent bookings (no gap)
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          val booking1 = createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-01"),
            LocalDate.parse("2024-04-10"),
          )

          cas3v2TurnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withWorkingDayCount(2)
          }

          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-10"),
            LocalDate.parse("2024-04-20"),
          )

          val expectedReportRows = listOf(
            // 11. Adjacent bookings (no gap)
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-21,2024-04-30]",
              10,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report with Multiple voids per bed returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }

          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 12. Multiple voids or bookings per bed within the period
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bed1)
            withStartDate(LocalDate.parse("2024-04-01"))
            withEndDate(LocalDate.parse("2024-04-05"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bed1)
            withStartDate(LocalDate.parse("2024-04-10"))
            withEndDate(LocalDate.parse("2024-04-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val expectedReportRows = listOf(
            // 12. Multiple voids or bookings per bed within the period
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-06,2024-04-09]",
              4,
              0,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-16,2024-04-30]",
              15,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bed with no bookings or voids returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          // 13. Bed with no bookings or voids
          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)

          val expectedReportRows = listOf(
            // 13. Bed with no bookings or voids
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-01,2024-04-30]",
              30,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report for bookings with turnaround time returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }
          val bedStart = LocalDate.parse("2024-01-01")
          val bedEnd = LocalDate.parse("2024-12-31")

          val bed1 = createBedspace(premises, "bed1", bedStart, bedEnd)
          val booking1 = createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-01"),
            LocalDate.parse("2024-04-05"),
          )

          cas3v2TurnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withWorkingDayCount(2)
          }

          createBooking(
            premises,
            bed1,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-15"),
            LocalDate.parse("2024-04-20"),
          )

          val expectedReportRows = listOf(
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-10,2024-04-14]",
              5,
              4,
            ),
            Cas3BookingGapReportRow(
              premises.probationDeliveryUnit.probationRegion.name,
              probationDeliveryUnit.name,
              premises.name,
              bed1.reference,
              "[2024-04-21,2024-04-30]",
              10,
              null,
            ),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedReportRows.toDataFrame())
            }
        }
      }
    }

    @Test
    fun `Get booking gap report with zero beds returns OK`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 4, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(probationDeliveryUnit)
          }

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas3BookingGapReportRow>(Remove)
              assertThat(actual.toString()).isEqualTo(emptyList<Cas3BookingGapReportRow>().toDataFrame().toString())
            }
        }
      }
    }

    private fun createBedspace(premises: Cas3PremisesEntity, roomName: String, startDate: LocalDate, endDate: LocalDate) = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withReference(roomName)
      withStartDate(startDate)
      withEndDate(endDate)
      withCreatedAt(OffsetDateTime.of(startDate, LocalTime.MIDNIGHT, ZoneOffset.UTC))
    }

    private fun createBooking(
      premises: Cas3PremisesEntity,
      bedspace: Cas3BedspacesEntity,
      crn: String,
      arrivalDate: LocalDate,
      departureDate: LocalDate,
    ): Cas3BookingEntity = cas3BookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBedspace(bedspace)
      withServiceName(ServiceName.temporaryAccommodation)
      withCrn(crn)
      withArrivalDate(arrivalDate)
      withDepartureDate(departureDate)
    }
  }

  private fun setupPremisesWIthABedspaceAndABooking(
    crn: String,
    user: UserEntity,
    startDate: LocalDate,
    arrivalDate: LocalDate = LocalDate.of(2023, 4, 5),
    departureDate: LocalDate = LocalDate.of(2023, 4, 7),
  ): Triple<Cas3PremisesEntity, Cas3BedspacesEntity, Cas3BookingEntity> {
    val premises = givenACas3Premises(
      user.probationRegion,
      status = Cas3PremisesStatus.online,
    )
    val bedspaceStartDate = startDate.minusDays(100)
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withStartDate(bedspaceStartDate)
      withCreatedDate(bedspaceStartDate)
      withEndDate(null)
    }
    val booking = cas3BookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premises)
      withBedspace(bedspace)
      withCrn(crn)
      withArrivalDate(arrivalDate)
      withDepartureDate(departureDate)
    }
    return Triple(premises, bedspace, booking)
  }

  private fun assertCorrectPersonDetail(
    expectedCaseSummary: CaseSummary,
    actualReferralRow: TransitionalAccommodationReferralReportRow,
  ) {
    val expectedName =
      (listOf(expectedCaseSummary.name.forename) + expectedCaseSummary.name.middleNames + expectedCaseSummary.name.surname).joinToString(
        " ",
      )

    val expectedGenderIdentity =
      if (actualReferralRow.genderIdentity == "Prefer to self-describe") expectedCaseSummary.profile?.selfDescribedGender else expectedCaseSummary.profile?.genderIdentity

    assertThat(actualReferralRow.crn).isEqualTo(expectedCaseSummary.crn)
    assertThat(actualReferralRow.sex).isEqualTo(expectedCaseSummary.gender)
    assertThat(actualReferralRow.genderIdentity).isEqualTo(expectedGenderIdentity)
    assertThat(actualReferralRow.dateOfBirth).isEqualTo(expectedCaseSummary.dateOfBirth)
    assertThat(actualReferralRow.ethnicity).isEqualTo(expectedCaseSummary.profile?.ethnicity)
    assertThat(actualReferralRow.pncNumber).isEqualTo(expectedCaseSummary.pnc)
    assertThat(actualReferralRow.personName).isEqualTo(expectedName)
  }

  private fun assertCorrectReferralDetails(
    expectedAssessment: TemporaryAccommodationAssessmentEntity,
    actualReferralReportRow: TransitionalAccommodationReferralReportRow,
    expectedReferralRejected: String,
  ) {
    val application = expectedAssessment.application as TemporaryAccommodationApplicationEntity
    val isAssessmentRejected = REJECTED.name == expectedAssessment.decision?.name
    val rejectedDate = if (isAssessmentRejected) expectedAssessment.submittedAt else null

    assertThat(actualReferralReportRow.referralId).isEqualTo(expectedAssessment.application.id.toString())
    assertThat(actualReferralReportRow.crn).isEqualTo(expectedAssessment.application.crn)
    assertThat(actualReferralReportRow.dutyToReferMade).isEqualTo(application.isDutyToReferSubmitted.toYesNo())
    assertThat(actualReferralReportRow.dateDutyToReferMade).isEqualTo(application.dutyToReferSubmissionDate)
    assertThat(actualReferralReportRow.dutyToReferLocalAuthorityAreaName).isEqualTo(application.dutyToReferLocalAuthorityAreaName)
    assertThat(actualReferralReportRow.historyOfArsonOffence).isEqualTo(application.hasHistoryOfArson.toYesNo())
    assertThat(actualReferralReportRow.concerningArsonBehaviour).isEqualTo(application.isConcerningArsonBehaviour.toYesNo())
    assertThat(actualReferralReportRow.needForAccessibleProperty).isEqualTo(application.needsAccessibleProperty.toYesNo())
    assertThat(actualReferralReportRow.referralDate).isEqualTo(application.createdAt.toLocalDate())
    assertThat(actualReferralReportRow.referralSubmittedDate).isEqualTo(application.submittedAt?.toLocalDate())
    assertThat(actualReferralReportRow.registeredSexOffender).isEqualTo(application.isRegisteredSexOffender.toYesNo())
    assertThat(actualReferralReportRow.historyOfSexualOffence).isEqualTo(application.isHistoryOfSexualOffence.toYesNo())
    assertThat(actualReferralReportRow.concerningSexualBehaviour).isEqualTo(application.isConcerningSexualBehaviour.toYesNo())
    assertThat(actualReferralReportRow.referralRejected).isEqualTo(expectedReferralRejected)
    assertThat(actualReferralReportRow.rejectionDate).isEqualTo(rejectedDate?.toLocalDate())
    assertThat(actualReferralReportRow.rejectionReason).isEqualTo(expectedAssessment.referralRejectionReason?.name)
    assertThat(actualReferralReportRow.rejectionReasonExplained).isEqualTo(expectedAssessment.referralRejectionReasonDetail)
    assertThat(actualReferralReportRow.accommodationRequiredDate).isEqualTo(application.arrivalDate?.toLocalDate())
    assertThat(actualReferralReportRow.updatedAccommodationRequiredFromDate).isNull()
    assertThat(actualReferralReportRow.prisonAtReferral).isEqualTo(application.prisonNameOnCreation)
    assertThat(actualReferralReportRow.releaseDate).isEqualTo(application.personReleaseDate)
    assertThat(actualReferralReportRow.updatedReleaseDate).isNull()
    assertThat(actualReferralReportRow.pdu).isEqualTo(application.probationDeliveryUnit?.name)
  }

  private fun createTemporaryAccommodationAssessmentForStatus(
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentStatus: AssessmentStatus,
    submittedDate: LocalDate,
  ): TemporaryAccommodationAssessmentEntity {
    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withArrivalDate(LocalDate.now().randomDateAfter(14))
      withSubmittedAt(submittedDate.atStartOfDay().atOffset(ZoneOffset.UTC))
      withCreatedAt(OffsetDateTime.now())
      withRiskRatings { PersonRisksFactory().produce() }
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withDecision(null)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())

      when (assessmentStatus) {
        AssessmentStatus.cas3Rejected -> {
          withDecision(REJECTED)
        }

        AssessmentStatus.cas3Closed -> {
          withDecision(ACCEPTED)
          withCompletedAt(OffsetDateTime.now())
        }

        cas3ReadyToPlace -> {
          withDecision(ACCEPTED)
        }

        cas3InReview -> {
          withAllocatedToUser(user)
        }

        cas3Unallocated -> {
        }

        else -> throw IllegalArgumentException("status $assessmentStatus is not supported")
      }
    }

    return assessment
  }

  @SuppressWarnings("LongParameterList")
  private fun createReferralAndAssessment(
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
    probationDeliveryUnit: ProbationDeliveryUnitEntity,
    applicationSubmittedDate: OffsetDateTime,
    accommodationRequiredDate: LocalDate,
    updatedAccommodationRequiredDate: LocalDate?,
  ): Triple<Cas3PremisesEntity, Cas3BedspacesEntity, TemporaryAccommodationApplicationEntity> {
    val premises = givenACas3Premises(
      user.probationRegion,
      status = Cas3PremisesStatus.online,
    )
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withArrivalDate(accommodationRequiredDate)
      withSubmittedAt(applicationSubmittedDate)
      withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(10))
      withDutyToReferLocalAuthorityAreaName("London")
      withDutyToReferSubmissionDate(LocalDate.now().randomDateAfter(30))
      withHasHistoryOfArson(randomBoolean())
      withIsDutyToReferSubmitted(randomBoolean())
      withHasRegisteredSexOffender(randomBoolean())
      withHasHistoryOfSexualOffence(randomBoolean())
      withIsConcerningSexualBehaviour(randomBoolean())
      withNeedsAccessibleProperty(randomBoolean())
      withPrisonNameAtReferral(randomStringMultiCaseWithNumbers(20))
      withPersonReleaseDate(LocalDate.now().randomDateAfter(30))
      withEligiblilityReason(randomStringLowerCase(20))
      withRiskRatings {
        withRoshRisks(
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
      }
    }
    temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withDecision(ACCEPTED)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withSubmittedAt(OffsetDateTime.now())
      withAccommodationRequiredFromDate(updatedAccommodationRequiredDate)
    }
    return Triple(premises, bedspace, application)
  }

  private fun randomBoolean() = randomInt(0, 20) > 10
}
