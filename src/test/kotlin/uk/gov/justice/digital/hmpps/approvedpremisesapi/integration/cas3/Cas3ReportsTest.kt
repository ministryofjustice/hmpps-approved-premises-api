package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns.Remove
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an AP Area`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toBookingsReportDataAndPersonInfo
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
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
    `Given a User` { user, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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

    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2023&month=4")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get CAS3 referral report returns 403 Forbidden if user role is CAS3_ASSESSOR role and the region is not allowed region`() {
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get CAS3 referral report OK response if user role is CAS3_ASSESSOR and requested regionId is allowed region`() {
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Get CAS3 referral report returns OK response if user is CAS3_REPORTER and allow access to all region when no region is requested `() {
      `Given a User`(roles = listOf(CAS3_REPORTER)) { _, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2024&month=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Get CAS3 referral report returns OK response if user is CAS3_REPORTER the request region not matched to user region`() {
      `Given a User`(roles = listOf(CAS3_REPORTER)) { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Get CAS3 referral report returns 403 Forbidden if a user does not have the CAS3_ASSESSOR role`() {
      `Given a User` { user, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get CAS3 referral report returns 400 if month is provided and not within 1-12`() {
      `Given a User`(roles = listOf(CAS3_REPORTER)) { _, jwt ->
        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2023&month=-1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
      }
    }

    @ParameterizedTest
    @EnumSource
    @NullSource
    fun `Get CAS3 referral report successfully with single matching referral in the report with different 'assessmentDecision'`(
      assessmentDecision: AssessmentDecision?,
    ) {
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

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
            withProbationDeliveryUnit(user.probationDeliveryUnit)
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(userRole)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val (premises, application) = createReferralAndAssessment(user, offenderDetails)

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val (premises, application) = createReferralAndAssessment(user, offenderDetails)

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2025&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient.get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            user.deliusUsername,
          )

          webTestClient
            .get()
            .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
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
  }

  @Nested
  inner class GetReferralReportNew {
    @Test
    fun `Get CAS3 referral report OK response if user role is CAS3_ASSESSOR and requested regionId is allowed region`() {
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
      `Given a User`(roles = listOf(CAS3_REPORTER)) { _, jwt ->
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
      `Given a User`(roles = listOf(CAS3_REPORTER)) { user, jwt ->
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(userRole)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val (premises, application) = createReferralAndAssessment(user, offenderDetails)

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val (premises, application) = createReferralAndAssessment(user, offenderDetails)

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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

    @Test
    fun `Get CAS3 referral report successfully when offender gender identity is Prefer to self-describe`() {
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            OffenderDetailsSummaryFactory()
              .withGenderIdentity("Prefer to self-describe")
              .withSelfDescribedGenderIdentity(randomStringLowerCase(10))
              .produce()
          },
        ) { offenderDetails, _ ->

          val (premises, application) = createReferralAndAssessment(user, offenderDetails)

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_REPORTER)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_REPORTER)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_REPORTER)) { userEntity, jwt ->

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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
                  `Given an AP Area`()
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
              bookingStatus = BookingStatus.provisional,
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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
              bookingStatus = BookingStatus.provisional,
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
  inner class GetBedUtilizationReport {
    @Test
    fun `Get bed utilisation report returns OK with correct body`() {
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
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

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given a User`(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }

          val (premises, room) = createPremisesAndRoom(userEntity.probationRegion, probationDeliveryUnit)
          val bed = createBed(room)

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
      `Given an Offender` { offenderDetails, inmateDetails ->
        val yorkshireRegion = probationRegionRepository.findByName("Yorkshire & The Humber")

        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(yorkshireRegion!!)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(yorkshireRegion!!)
          withProbationDeliveryUnit(probationDeliveryUnit)
        }

        val booking1DepartureDate = LocalDate.of(2023, 4, 12)

        val bedOne = bedEntityFactory.produceAndPersist {
          withRoom(
            roomEntityFactory.produceAndPersist {
              withPremises(premises)
              withName("room1")
            },
          )
        }

        val booking1 = createBooking(premises, bedOne, offenderDetails.otherIds.crn, LocalDate.of(2023, 4, 5), booking1DepartureDate)

        val turnaroundBooking1 = turnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withWorkingDayCount(5)
        }

        booking1.turnarounds = mutableListOf(turnaroundBooking1)

        confirmationEntityFactory.produceAndPersist {
          withBooking(booking1)
        }

        val booking2DepartureDate = LocalDate.of(2023, 4, 21)

        val bedTwo = bedEntityFactory.produceAndPersist {
          withRoom(
            roomEntityFactory.produceAndPersist {
              withPremises(premises)
              withName("room2")
            },
          )
        }

        val booking2 = createBooking(premises, bedTwo, offenderDetails.otherIds.crn, LocalDate.of(2023, 4, 19), booking2DepartureDate)

        val turnaroundBooking2 = turnaroundFactory.produceAndPersist {
          withBooking(booking2)
          withWorkingDayCount(2)
        }

        booking2.turnarounds = mutableListOf(turnaroundBooking2)

        confirmationEntityFactory.produceAndPersist {
          withBooking(booking2)
        }

        val booking3 = createBooking(premises, bedTwo, offenderDetails.otherIds.crn, LocalDate.of(2024, 3, 8), LocalDate.of(2024, 4, 21))

        cancellationEntityFactory.produceAndPersist {
          withBooking(booking3)
          withYieldedReason {
            cancellationReasonEntityFactory.produceAndPersist()
          }
        }

        val booking4ArrivalDate = LocalDate.of(2024, 5, 12)
        val booking4DepartureDate = LocalDate.of(2024, 7, 17)
        val booking4 = createBooking(premises, bedTwo, offenderDetails.otherIds.crn, booking4ArrivalDate, booking4DepartureDate)

        val turnaroundBooking4 = turnaroundFactory.produceAndPersist {
          withBooking(booking4)
          withWorkingDayCount(0)
        }

        booking4.turnarounds = mutableListOf(turnaroundBooking4)

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        val gapRangesReport = cas3ReportService.createBookingGapRangesReport()

        val today = LocalDate.now()

        val expectedGapRangesReport = listOf(
          mutableMapOf<String, Any?>(
            "probation_region" to premises.probationRegion.name,
            "pdu_name" to probationDeliveryUnit.name,
            "premises_name" to premises.name,
            "bed_name" to "room2",
            "gap" to "[${booking2DepartureDate.plusDays(1)},$booking4ArrivalDate)",
            "gap_days" to ChronoUnit.DAYS.between(booking2DepartureDate.plusDays(1), booking4ArrivalDate).toInt(),
            "turnaround_days" to 2,
          ),
        )

        assertThat(gapRangesReport).isEqualTo(expectedGapRangesReport)
      }
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

  private fun createReferralAndAssessment(
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
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
    }

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withApplicationSchema(applicationSchema)
      withArrivalDate(LocalDate.now().randomDateAfter(14))
      withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
      withCreatedAt(OffsetDateTime.now())
      withDutyToReferLocalAuthorityAreaName("London")
      withDutyToReferSubmissionDate(LocalDate.now())
      withHasHistoryOfArson(true)
      withIsDutyToReferSubmitted(true)
      withHasRegisteredSexOffender(true)
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
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withDecision(ACCEPTED)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withSubmittedAt(OffsetDateTime.now())
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
}
