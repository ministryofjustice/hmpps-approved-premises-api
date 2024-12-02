package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3InReview
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3ReadyToPlace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3Unallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.FutureBookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toYesNo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toBookingsReportDataAndPersonInfo
import java.io.StringReader
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas3ReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Autowired
  lateinit var cas3ReportService: Cas3ReportService

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report for all regions returns 403 Forbidden if user does not have all regions access`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("rangeTooLarge")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.endDate")
    }

    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-04-01"
      val endDate = "2023-07-01"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("rangeTooLarge")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.endDate")
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("afterEndDate")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.startDate")
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("afterEndDate")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.startDate")
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("inFuture")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.endDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if mandatory dates are not provided`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectBody()

      assertThat(actualBody.returnResult().status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  @Nested
  inner class GetReferralReport {
    @Test
    fun `Get CAS3 referral report returns 403 Forbidden if user does not have CAS3 role`() {
      givenAUser { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referral?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get CAS3 referral report returns 403 Forbidden if user role is CAS3_ASSESSOR role and the region is not allowed region`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referral?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get CAS3 referral report OK response if user role is CAS3_ASSESSOR and requested regionId is allowed region`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referral?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
            withAddedAt(OffsetDateTime.now())
          }

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withProbationDeliveryUnit(user.probationDeliveryUnit)
            withApplicationSchema(applicationSchema)
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
            withAssessmentSchema(assessmentSchema)
            withDecision(assessmentDecision)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            assessmentDecision?.let { withSubmittedAt(OffsetDateTime.now()) }
            withRejectionRationale(if (REJECTED.name == assessmentDecision?.name) "some reason" else null)
          }
          assessment.schemaUpToDate = true

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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val (premises, application) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC),
            LocalDate.now().randomDateAfter(30),
            null,
          )

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val (premises, application) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC),
            LocalDate.now().randomDateAfter(30),
            null,
          )

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAssessmentSchema(applicationSchema)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withSubmittedAt(OffsetDateTime.now())
          }

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
            withAddedAt(OffsetDateTime.now())
          }

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withProbationDeliveryUnit(user.probationDeliveryUnit)
            withApplicationSchema(applicationSchema)
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
            withAssessmentSchema(assessmentSchema)
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

          assessment.schemaUpToDate = true

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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
            withAddedAt(OffsetDateTime.now())
          }

          val prisonReleaseTypes = "Standard recall,CRD licence,ECSL"
          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withProbationDeliveryUnit(user.probationDeliveryUnit)
            withApplicationSchema(applicationSchema)
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
            withAssessmentSchema(assessmentSchema)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            REJECTED.let { withSubmittedAt(OffsetDateTime.now()) }
            withRejectionRationale("some reason")
          }

          assessment.schemaUpToDate = true

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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val applicationSchema =
            temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

          val assessmentSchema =
            temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }

          val prisonReleaseTypes = "Standard recall,CRD licence,ECSL"
          val application =
            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(user)
              withProbationRegion(user.probationRegion)
              withApplicationSchema(applicationSchema)
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
              withPdu("Probation Delivery Unit Test")
            }

          val assessment =
            temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
              withApplication(application)
              withAssessmentSchema(assessmentSchema)
              withDecision(REJECTED)
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              REJECTED.let { withSubmittedAt(OffsetDateTime.now()) }
              withRejectionRationale("some reason")
              withReleaseDate(LocalDate.now().plusDays(9))
              withAccommodationRequiredFromDate(LocalDate.now().plusDays(10))
            }

          assessment.schemaUpToDate = true

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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

          val (premises, application) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC),
            LocalDate.now().randomDateAfter(30),
            null,
          )

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAssessmentSchema(applicationSchema)
            withDecision(REJECTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withSubmittedAt(OffsetDateTime.now())
          }

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
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
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }

          bookings[2].let {
            val firstArrivalUpdate = arrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now().randomDateBefore(14))
            }
            val secondArrivalUpdate = arrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now())
            }

            it.arrivals = listOf(firstArrivalUpdate, secondArrivalUpdate).toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()

            val firstDepartureUpdate = departureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now().randomDateTimeBefore(14))
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            val secondDepartureUpdate = departureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now())
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            it.departures = listOf(firstDepartureUpdate, secondDepartureUpdate).toMutableList()
          }
          bookings[3].let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
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
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_REPORTER)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
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
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_REPORTER)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val pdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
            withProbationDeliveryUnit(pdu)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
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
            userEntity.deliusUsername,
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_REPORTER)) { userEntity, jwt ->

        webTestClient.get()
          .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report returns OK with only Bookings with at least one day in month when year and month are specified`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val shouldNotBeIncludedBookings = mutableListOf<BookingEntity>()
          val shouldBeIncludedBookings = mutableListOf<BookingEntity>()

          // Straddling start of month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 29))
            withDepartureDate(LocalDate.of(2023, 4, 1))
          }

          // Straddling end of month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 2))
            withDepartureDate(LocalDate.of(2023, 4, 3))
          }

          // Entirely within month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 30))
            withDepartureDate(LocalDate.of(2023, 5, 15))
          }

          // Encompassing month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 5, 28))
          }

          // Before month
          shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 3, 30))
          }

          // After month
          shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
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
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              shouldBeIncludedBookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val unexpectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea {
                  givenAnApArea()
                }
              }
            }
          }

          // Unexpected bookings
          bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(unexpectedPremises)
            withServiceName(ServiceName.temporaryAccommodation)
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
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val accommodationApplication =
            createTemporaryAccommodationApplication(offenderDetails, userEntity)

          val bookings = bookingEntityFactory.produceAndPersistMultiple(1) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
            withApplication(accommodationApplication)
          }
          bookings[0].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
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
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
  }

  @Nested
  inner class GetBedUsageReport {
    @Test
    fun `Get bed usage report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-04-05"),
            LocalDate.parse("2023-04-15"),
          )

          val expectedReportRows = listOf(
            BedUsageReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              crn = offenderDetails.otherIds.crn,
              type = BedUsageType.Booking,
              startDate = LocalDate.parse("2023-04-05"),
              endDate = LocalDate.parse("2023-04-15"),
              durationOfBookingDays = 10,
              bookingStatus = BookingStatus.PROVISIONAL,
              voidCategory = null,
              voidNotes = null,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedUsage?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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

    @Test
    fun `Get bed usage report returns OK with correct body with pdu and local authority`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-04-05"),
            LocalDate.parse("2023-04-15"),
          )

          val expectedReportRows = listOf(
            BedUsageReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              crn = offenderDetails.otherIds.crn,
              type = BedUsageType.Booking,
              startDate = LocalDate.parse("2023-04-05"),
              endDate = LocalDate.parse("2023-04-15"),
              durationOfBookingDays = 10,
              bookingStatus = BookingStatus.PROVISIONAL,
              voidCategory = null,
              voidNotes = null,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedUsage?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
  inner class GetBedUtilReport {
    @Test
    fun `Get bed utilisation report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-25"),
            LocalDate.parse("2023-04-17"),
          )

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 17,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK with correct body with pdu and local authority`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-04-05"),
            LocalDate.parse("2023-04-15"),
          )

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 11,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly bookedDaysActiveAndClosed the total number of days for Bookings that are marked as arrived`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-25"),
            LocalDate.parse("2023-04-17"),
          )

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking)
            withArrivalDate(LocalDate.parse("2023-03-25"))
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit?.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 17,
              confirmedDays = 0,
              provisionalDays = 0,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 17,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.5666666666666667,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly bookedDaysActiveAndClosed when there are multiple cancellations in the same period`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking1 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-02"),
            LocalDate.parse("2023-05-25"),
          )

          val booking2 = createBooking(
            premises,
            bed,
            randomStringLowerCase(10),
            LocalDate.parse("2023-02-18"),
            LocalDate.parse("2023-05-11"),
          )

          val booking3 = createBooking(
            premises,
            bed,
            randomStringLowerCase(10),
            LocalDate.parse("2023-02-15"),
            LocalDate.parse("2023-05-08"),
          )

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking1)
            withArrivalDate(LocalDate.parse("2023-03-02"))
            withExpectedDepartureDate(LocalDate.parse("2023-05-25"))
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking2)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking3)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withWorkingDayCount(2)
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withWorkingDayCount(5)
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withWorkingDayCount(2)
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking2)
            withWorkingDayCount(2)
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking3)
            withWorkingDayCount(2)
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit?.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 30,
              confirmedDays = 0,
              provisionalDays = 0,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 30,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 1.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly bookedDaysActiveAndClosed when there are multiple bookings in the same period`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2023-04-28"))
            withEndDate(LocalDate.parse("2023-05-04"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          }

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking1 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-25"),
            LocalDate.parse("2023-04-05"),
          )

          val booking2 = createBooking(
            premises,
            bed,
            randomStringLowerCase(10),
            LocalDate.parse("2023-04-07"),
            LocalDate.parse("2023-04-14"),
          )

          val booking3 = createBooking(
            premises,
            bed,
            randomStringLowerCase(10),
            LocalDate.parse("2023-04-08"),
            LocalDate.parse("2023-04-22"),
          )

          createBooking(
            premises,
            bed,
            randomStringLowerCase(10),
            LocalDate.parse("2023-04-24"),
            LocalDate.parse("2023-05-20"),
          )

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking1)
            withArrivalDate(LocalDate.parse("2023-03-25"))
            withExpectedDepartureDate(LocalDate.parse("2023-04-05"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withCreatedAt(OffsetDateTime.parse("2023-02-25T16:00:00+01:00"))
            withWorkingDayCount(2)
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withCreatedAt(OffsetDateTime.parse("2023-02-12T17:00:00+01:00"))
            withWorkingDayCount(5)
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking2)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking3)
            withArrivalDate(LocalDate.parse("2023-04-08"))
            withExpectedDepartureDate(LocalDate.parse("2023-04-22"))
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit?.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 20,
              confirmedDays = 0,
              provisionalDays = 7,
              scheduledTurnaroundDays = 2,
              effectiveTurnaroundDays = 2,
              voidDays = 3,
              totalBookedDays = 20,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.6666666666666666,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly bookedDaysActiveAndClosed when there are multiple booking arrivals in the same period`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-04-05"),
            LocalDate.parse("2024-06-04"),
          )

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking)
            withArrivalDate(LocalDate.parse("2024-04-05"))
            withExpectedDepartureDate(LocalDate.parse("2024-06-28"))
            withCreatedAt(OffsetDateTime.parse("2024-04-06T08:34:56.789Z"))
          }

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking)
            withArrivalDate(LocalDate.parse("2024-04-07"))
            withExpectedDepartureDate(LocalDate.parse("2024-06-30"))
            withCreatedAt(OffsetDateTime.parse("2024-04-06T09:57:21.789Z"))
          }

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking)
            withArrivalDate(LocalDate.parse("2024-04-06"))
            withExpectedDepartureDate(LocalDate.parse("2024-06-27"))
            withCreatedAt(OffsetDateTime.parse("2024-04-06T09:53:17.789Z"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking)
            withCreatedAt(OffsetDateTime.parse("2024-03-28T17:00:00+01:00"))
            withWorkingDayCount(7)
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit?.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 26,
              confirmedDays = 0,
              provisionalDays = 0,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 26,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.8666666666666667,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2024-04-01&endDate=2024-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly confirmedDays the total number of days for Bookings that are marked as confirmed but not arrived`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-25"),
            LocalDate.parse("2023-04-10"),
          )

          confirmationEntityFactory.produceAndPersist {
            withBooking(booking)
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 10,
              provisionalDays = 0,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly scheduledTurnaroundDays the number of working days in the report period for the turnaround`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-04-07"),
            LocalDate.parse("2023-04-21"),
          )

          turnaroundFactory.produceAndPersist {
            withBooking(booking)
            withWorkingDayCount(5)
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 15,
              scheduledTurnaroundDays = 5,
              effectiveTurnaroundDays = 7,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly effectiveTurnaroundDays the total number of days in the report period for the turnaround`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-25"),
            LocalDate.parse("2023-04-17"),
          )

          turnaroundFactory.produceAndPersist {
            withBooking(booking)
            withWorkingDayCount(5)
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 17,
              scheduledTurnaroundDays = 5,
              effectiveTurnaroundDays = 7,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly scheduledTurnaroundDays and effectiveTurnaroundDays when there are multiple bookings`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking1 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-06-24"),
            LocalDate.parse("2024-09-16"),
          )

          val booking2 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-03-21"),
            LocalDate.parse("2024-06-13"),
          )

          val booking3 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-03-22"),
            LocalDate.parse("2024-06-12"),
          )

          val booking4 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-03-15"),
            LocalDate.parse("2024-06-07"),
          )

          val booking5 = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2024-03-12"),
            LocalDate.parse("2024-06-04"),
          )

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking3)
            withArrivalDate(LocalDate.parse("2024-03-22"))
            withExpectedDepartureDate(LocalDate.parse("2024-06-14"))
            withCreatedAt(OffsetDateTime.parse("2024-03-25T09:23:17.789Z"))
          }

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking1)
            withArrivalDate(LocalDate.parse("2024-06-24"))
            withExpectedDepartureDate(LocalDate.parse("2024-09-16"))
            withCreatedAt(OffsetDateTime.parse("2024-06-28T08:31:17.789Z"))
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking4)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking2)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking5)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking5)
            withWorkingDayCount(7)
            withCreatedAt(OffsetDateTime.parse("2024-03-06T10:45:00+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking4)
            withWorkingDayCount(7)
            withCreatedAt(OffsetDateTime.parse("2024-03-13T09:34:00+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking2)
            withWorkingDayCount(7)
            withCreatedAt(OffsetDateTime.parse("2024-03-13T14:13:00+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking3)
            withWorkingDayCount(7)
            withCreatedAt(OffsetDateTime.parse("2024-03-13T16:43:00+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking3)
            withWorkingDayCount(8)
            withCreatedAt(OffsetDateTime.parse("2024-06-13T09:23:00+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking1)
            withWorkingDayCount(7)
            withCreatedAt(OffsetDateTime.parse("2024-06-17T13:55:00+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking3)
            withWorkingDayCount(5)
            withCreatedAt(OffsetDateTime.parse("2024-06-18T09:42:27+01:00"))
          }

          turnaroundFactory.produceAndPersist {
            withBooking(booking3)
            withWorkingDayCount(3)
            withCreatedAt(OffsetDateTime.parse("2024-06-18T09:42:37+01:00"))
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 19,
              confirmedDays = 0,
              provisionalDays = 0,
              scheduledTurnaroundDays = 3,
              effectiveTurnaroundDays = 5,
              voidDays = 0,
              totalBookedDays = 19,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.6333333333333333,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2024-06-01&endDate=2024-06-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly voidDays the total number of days in the month for voids`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2023-03-28"))
            withEndDate(LocalDate.parse("2023-04-04"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          }

          lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2023-04-25"))
            withEndDate(LocalDate.parse("2023-05-03"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 0,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 10,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK and shows correctly totalBookedDays`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises,
            bed,
            offenderDetails.otherIds.crn,
            LocalDate.parse("2023-03-28"),
            LocalDate.parse("2023-04-04"),
          )

          turnaroundFactory.produceAndPersist {
            withBooking(booking)
            withWorkingDayCount(5)
          }

          arrivalEntityFactory.produceAndPersist {
            withBooking(booking)
            withArrivalDate(LocalDate.parse("2023-03-28"))
          }

          departureEntityFactory.produceAndPersist {
            withBooking(booking)
            withDateTime(OffsetDateTime.parse("2023-04-04T12:00:00.000Z"))
            withReason(departureReasonEntityFactory.produceAndPersist())
            withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 4,
              confirmedDays = 0,
              provisionalDays = 0,
              scheduledTurnaroundDays = 5,
              effectiveTurnaroundDays = 7,
              voidDays = 0,
              totalBookedDays = 4,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.13333333333333333,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(Remove)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }

  @Nested
  inner class GetBookingGapReport {

    @Test
    fun `Get booking gap report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val reportStartDate = LocalDate.of(2024, 4, 1)
          val reportEndDate = LocalDate.of(2024, 6, 30)

          val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(yorkshireRegion!!)
          }

          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(yorkshireRegion!!)
            withProbationDeliveryUnit(probationDeliveryUnit)
          }

          val bedOne = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
                withName("room1")
              },
            )
          }

          val booking1 = createBooking(
            premises,
            bedOne,
            offenderDetails.otherIds.crn,
            LocalDate.of(2024, 4, 5),
            LocalDate.of(2024, 4, 12),
          )

          confirmationEntityFactory.produceAndPersist {
            withBooking(booking1)
          }

          val bedTwo = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
                withName("room2")
              },
            )
          }

          val booking2 = createBooking(
            premises,
            bedTwo,
            offenderDetails.otherIds.crn,
            LocalDate.of(2024, 4, 19),
            LocalDate.of(2024, 4, 21),
          )

          confirmationEntityFactory.produceAndPersist {
            withBooking(booking2)
          }

          val booking3 = createBooking(
            premises,
            bedTwo,
            offenderDetails.otherIds.crn,
            LocalDate.of(2024, 3, 8),
            LocalDate.of(2024, 4, 21),
          )

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking3)
            withYieldedReason {
              cancellationReasonEntityFactory.produceAndPersist()
            }
          }

          val booking4 = createBooking(
            premises,
            bedTwo,
            offenderDetails.otherIds.crn,
            LocalDate.of(2024, 5, 12),
            LocalDate.of(2024, 7, 17),
          )

          webTestClient.get()
            .uri("/cas3/reports/bookingGap?startDate=$reportStartDate&endDate=$reportEndDate&probationRegionId=${user.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
                .convertTo<BookingGapReportRow>()
                .toList()

              assertThat(actual.size).isEqualTo(4)

              assertReportRow(
                actual[0],
                premises.probationRegion.name,
                probationDeliveryUnit.name,
                premises.name,
                bedOne.room.name,
                reportStartDate,
                booking1.arrivalDate,
              )

              assertReportRow(
                actual[1],
                premises.probationRegion.name,
                probationDeliveryUnit.name,
                premises.name,
                bedOne.room.name,
                booking1.departureDate.plusDays(1),
                reportEndDate,
              )

              assertReportRow(
                actual[2],
                premises.probationRegion.name,
                probationDeliveryUnit.name,
                premises.name,
                bedTwo.room.name,
                reportStartDate,
                booking2.arrivalDate,
              )

              assertReportRow(
                actual[3],
                premises.probationRegion.name,
                probationDeliveryUnit.name,
                premises.name,
                bedTwo.room.name,
                booking2.departureDate.plusDays(1),
                booking4.arrivalDate,
              )
            }
        }
      }
    }

    private fun assertReportHeader(headers: List<String>) {
      assertThat(headers).contains("probation_region")
      assertThat(headers).contains("pdu_name")
      assertThat(headers).contains("premises_name")
      assertThat(headers).contains("bed_name")
      assertThat(headers).contains("gap")
      assertThat(headers).contains("gap_days")
      assertThat(headers).contains("turnaround_days")
    }

    @Suppress("LongParameterList")
    private fun assertReportRow(
      row: BookingGapReportRow,
      probationRegionName: String,
      probationDeliveryUnitName: String,
      premisesName: String,
      bedName: String,
      gapStartDate: LocalDate,
      gapEndDate: LocalDate,
    ) {
      assertThat(row.probation_region).isEqualTo(probationRegionName)
      assertThat(row.pdu_name).isEqualTo(probationDeliveryUnitName)
      assertThat(row.premises_name).isEqualTo(premisesName)
      assertThat(row.bed_name).isEqualTo(bedName)
      assertThat(row.gap).isEqualTo("[$gapStartDate,$gapEndDate)")
      assertThat(row.gap_days).isEqualTo(ChronoUnit.DAYS.between(gapStartDate, gapEndDate).toString())
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
          val (premisesOne, applicationOne) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesOneAccommodationRequiredDate,
            null,
          )

          val premisesTwoAccommodationRequiredDate = reportStartDate.plusDays(21)
          val (premisesTwo, applicationTwo) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesTwoAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date before report start date
          val premisesThreeAccommodationRequiredDate = reportStartDate.minusDays(60)
          val (premisesThree, applicationThree) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesThreeAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date out of report dates range
          val premisesFourAccommodationRequiredDate = reportEndDate.plusDays(200)
          val (premisesFour, applicationFour) = createReferralAndAssessment(
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
          val (premisesFive, applicationFive) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesFiveAccommodationRequiredDate,
            premisesFiveUpdatedAccommodationRequiredDate,
          )

          val premisesSixAccommodationRequiredDate = reportStartDate.plusDays(210)
          val premisesSixUpdatedAccommodationRequiredDate = reportEndDate.plusDays(9)
          val (premisesSix, applicationSix) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesSixAccommodationRequiredDate,
            premisesSixUpdatedAccommodationRequiredDate,
          )

          // offender with updated accommodation required date out of report dates range
          val premisesSevenAccommodationRequiredDate = reportStartDate.minusDays(6)
          val (premisesSeven, applicationSeven) = createReferralAndAssessment(
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
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(6),
            reportEndDate.plusDays(30),
          )

          val bookingTwo = createBooking(
            applicationFive,
            premisesFive,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(3),
            reportStartDate.plusDays(18),
          )

          val bookingThree = createBooking(
            applicationSix,
            premisesSix,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(13),
            reportStartDate.plusDays(56),
          )

          // old booking
          createBooking(
            applicationThree,
            premisesThree,
            offenderDetails.otherIds.crn,
            reportStartDate.minusDays(90),
            reportStartDate.minusDays(15),
          )

          // confirmed booking
          val bookingFour = createBooking(
            applicationTwo,
            premisesTwo,
            offenderDetails.otherIds.crn,
            reportStartDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(20),
          )
          bookingFour.let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // cancelled booking
          createBooking(
            applicationOne,
            premisesOne,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(8),
            reportStartDate.plusDays(30),
          ).let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }

          // future confirmed booking
          val bookingFive = createBooking(
            applicationFive,
            premisesFive,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(40),
          )
          bookingFive.let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // future booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(70),
            reportEndDate.randomDateAfter(90),
          )

          // future confirmed booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(90),
            reportEndDate.plusDays(120),
          ).let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          createBooking(
            applicationSeven,
            premisesSeven,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(70),
            reportEndDate.plusDays(85),
          ).let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          val (premisesOne, applicationOne) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesOneAccommodationRequiredDate,
            null,
          )

          val premisesTwoAccommodationRequiredDate = reportStartDate.plusDays(21)
          val (premisesTwo, applicationTwo) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesTwoAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date before report start date
          val premisesThreeAccommodationRequiredDate = reportStartDate.minusDays(60)
          val (premisesThree, applicationThree) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesThreeAccommodationRequiredDate,
            null,
          )

          // offender with accommodation required date out of report dates range
          val premisesFourAccommodationRequiredDate = reportEndDate.plusDays(200)
          val (premisesFour, applicationFour) = createReferralAndAssessment(
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
          val (premisesFive, applicationFive) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesFiveAccommodationRequiredDate,
            premisesFiveUpdatedAccommodationRequiredDate,
          )

          val premisesSixAccommodationRequiredDate = reportStartDate.plusDays(210)
          val premisesSixUpdatedAccommodationRequiredDate = reportEndDate.plusDays(9)
          val (premisesSix, applicationSix) = createReferralAndAssessment(
            user,
            offenderDetails,
            probationDeliveryUnit,
            OffsetDateTime.now().randomDateTimeBefore(10),
            premisesSixAccommodationRequiredDate,
            premisesSixUpdatedAccommodationRequiredDate,
          )

          // offender with updated accommodation required date out of report dates range
          val premisesSevenAccommodationRequiredDate = reportStartDate.minusDays(6)
          val (premisesSeven, applicationSeven) = createReferralAndAssessment(
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
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(6),
            reportEndDate.plusDays(30),
          )

          val bookingTwo = createBooking(
            applicationFive,
            premisesFive,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(3),
            reportStartDate.plusDays(18),
          )

          val bookingThree = createBooking(
            applicationSix,
            premisesSix,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(13),
            reportStartDate.plusDays(56),
          )

          // old booking
          createBooking(
            applicationThree,
            premisesThree,
            offenderDetails.otherIds.crn,
            reportStartDate.minusDays(90),
            reportStartDate.minusDays(15),
          )

          // confirmed booking
          val bookingFour = createBooking(
            applicationTwo,
            premisesTwo,
            offenderDetails.otherIds.crn,
            reportStartDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(20),
          )
          bookingFour.let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // cancelled booking
          createBooking(
            applicationOne,
            premisesOne,
            offenderDetails.otherIds.crn,
            reportStartDate.plusDays(8),
            reportStartDate.plusDays(30),
          ).let {
            it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }

          // future confirmed booking
          val bookingFive = createBooking(
            applicationFive,
            premisesFive,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(10),
            reportEndDate.randomDateAfter(40),
          )
          bookingFive.let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          // future booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            offenderDetails.otherIds.crn,
            reportEndDate.randomDateAfter(70),
            reportEndDate.randomDateAfter(90),
          )

          // future confirmed booking out of report dates range
          createBooking(
            applicationFour,
            premisesFour,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(90),
            reportEndDate.plusDays(120),
          ).let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
              withBooking(it)
            }
          }

          createBooking(
            applicationSeven,
            premisesSeven,
            offenderDetails.otherIds.crn,
            reportEndDate.plusDays(70),
            reportEndDate.plusDays(85),
          ).let {
            it.confirmation = confirmationEntityFactory.produceAndPersist {
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      premises: PremisesEntity,
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
      premises: PremisesEntity,
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
    assertThat(actualReferralReportRow.pdu).isEqualTo(application.pdu)
  }

  private fun createTemporaryAccommodationAssessmentForStatus(
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentStatus: AssessmentStatus,
    submittedDate: LocalDate,
  ): TemporaryAccommodationAssessmentEntity {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withApplicationSchema(applicationSchema)
      withArrivalDate(LocalDate.now().randomDateAfter(14))
      withSubmittedAt(submittedDate.atStartOfDay().atOffset(ZoneOffset.UTC))
      withCreatedAt(OffsetDateTime.now())
      withRiskRatings { PersonRisksFactory().produce() }
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
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
    assessment.schemaUpToDate = true

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
  ): Pair<TemporaryAccommodationPremisesEntity, TemporaryAccommodationApplicationEntity> {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withProbationRegion(user.probationRegion)
      withProbationDeliveryUnit(probationDeliveryUnit)
    }

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withApplicationSchema(applicationSchema)
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

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withDecision(ACCEPTED)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withSubmittedAt(OffsetDateTime.now())
      withAccommodationRequiredFromDate(updatedAccommodationRequiredDate)
    }
    assessment.schemaUpToDate = true
    return Pair(premises, application)
  }

  private fun createTemporaryAccommodationApplication(
    offenderDetails: OffenderDetailSummary,
    userEntity: UserEntity,
  ): TemporaryAccommodationApplicationEntity {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
    return temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(userEntity)
      withProbationRegion(userEntity.probationRegion)
      withApplicationSchema(applicationSchema)
      withDutyToReferLocalAuthorityAreaName("London")
      withSubmittedAt(OffsetDateTime.now())
    }
  }

  private fun createPremisesAndRoom(
    probationRegion: ProbationRegionEntity,
    probationDeliveryUnit: ProbationDeliveryUnitEntity,
  ): Pair<PremisesEntity, RoomEntity> {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withLocalAuthorityArea(localAuthorityArea)
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    return Pair(premises, room)
  }

  private fun createBed(room: RoomEntity): BedEntity {
    return bedEntityFactory.produceAndPersist {
      withRoom(room)
    }
  }

  private fun createBooking(
    application: TemporaryAccommodationApplicationEntity,
    premises: PremisesEntity,
    crn: String,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
  ): BookingEntity {
    return bookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPremises(premises)
      withServiceName(ServiceName.temporaryAccommodation)
      withCrn(crn)
      withArrivalDate(arrivalDate)
      withDepartureDate(departureDate)
    }
  }

  private fun createBooking(
    premises: PremisesEntity,
    bed: BedEntity,
    crn: String,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
  ): BookingEntity {
    return bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bed)
      withServiceName(ServiceName.temporaryAccommodation)
      withCrn(crn)
      withArrivalDate(arrivalDate)
      withDepartureDate(departureDate)
    }
  }

  private fun randomBoolean() = randomInt(0, 20) > 10

  @SuppressWarnings("ConstructorParameterNaming")
  data class BookingGapReportRow(
    val probation_region: String,
    val pdu_name: String,
    val premises_name: String,
    val bed_name: String,
    val gap: String,
    val gap_days: String?,
  )
}
