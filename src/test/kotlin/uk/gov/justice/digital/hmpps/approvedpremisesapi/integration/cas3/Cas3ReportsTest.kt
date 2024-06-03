package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns.Remove
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toYesNo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
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
import java.util.UUID

class Cas3ReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Autowired
  lateinit var realBookingRepository: BookingRepository

  @Autowired
  lateinit var realLostBedsRepository: LostBedsRepository

  @Autowired
  lateinit var realWorkingDayCountService: WorkingDayService

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
      givenAUser { _, jwt ->
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
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
      givenAUser(roles = listOf(CAS3_REPORTER)) { _, jwt ->
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
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->
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
      givenAUser { user, jwt ->
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
      givenAUser(roles = listOf(CAS3_REPORTER)) { _, jwt ->
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
            withPdu("Probation Delivery Unit Test")
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
              assertCorrectReferralDetails(assessment, actual[0])
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
              )
              assertCorrectReferralDetails(
                assessmentUnAllocated,
                actual.find { it.referralId == assessmentUnAllocated.application.id.toString() }!!,
              )
              assertCorrectReferralDetails(
                assessmentReadyToPlace,
                actual.find { it.referralId == assessmentReadyToPlace.application.id.toString() }!!,
              )
            }
        }
      }
    }

    @Test
    fun `Get CAS3 referral successfully with referral has been offered with booking`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

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

          apDeliusContextAddResponseToUserAccessCall(
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

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

          apDeliusContextAddResponseToUserAccessCall(
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
            withPdu("Probation Delivery Unit Test")
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

          assessment.schemaUpToDate = true

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
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
              assertCorrectReferralDetails(assessment, actual[0])
            }

          assertThat(referralRejectionReason).isNotNull()
        }
      }
    }
  }

  @Nested
  inner class GetReferralReportNew {
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
            withPdu("Probation Delivery Unit Test")
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
              assertCorrectReferralDetails(assessment, actual[0])
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
              )
              assertCorrectReferralDetails(
                assessmentUnAllocated,
                actual.find { it.referralId == assessmentUnAllocated.application.id.toString() }!!,
              )
              assertCorrectReferralDetails(
                assessmentReadyToPlace,
                actual.find { it.referralId == assessmentReadyToPlace.application.id.toString() }!!,
              )
            }
        }
      }
    }

    @Test
    fun `Get CAS3 referral successfully with referral has been offered with booking`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

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

          apDeliusContextAddResponseToUserAccessCall(
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->

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

          apDeliusContextAddResponseToUserAccessCall(
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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender(
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

          apDeliusContextAddResponseToUserAccessCall(
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
              withArrivalDate(LocalDate.now().randomDateBefore())
            }
            val secondArrivalUpdate = arrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now())
            }

            it.arrivals = listOf(firstArrivalUpdate, secondArrivalUpdate).toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()

            val firstDepartureUpdate = departureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now().randomDateTimeBefore())
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              shouldBeIncludedBookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
                  apAreaEntityFactory.produceAndPersist()
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
        givenAnOffender { offenderDetails, inmateDetails ->
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
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
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
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedDataFrame = BedUsageReportGenerator(
            bookingTransformer,
            realBookingRepository,
            realLostBedsRepository,
            realWorkingDayCountService,
          )
            .createReport(
              listOf(bed),
              BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

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
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
            withProbationDeliveryUnit(probationDeliveryUnit)
            withLocalAuthorityArea(localAuthorityArea)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedDataFrame = BedUsageReportGenerator(
            bookingTransformer,
            realBookingRepository,
            realLostBedsRepository,
            realWorkingDayCountService,
          )
            .createReport(
              listOf(bed),
              BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

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
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedDataFrame = BedUtilisationReportGenerator(
            realBookingRepository,
            realLostBedsRepository,
            realWorkingDayCountService,
          )
            .createReport(
              listOf(bed),
              BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

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
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
            withProbationDeliveryUnit(probationDeliveryUnit)
            withLocalAuthorityArea(localAuthorityArea)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedDataFrame = BedUtilisationReportGenerator(
            realBookingRepository,
            realLostBedsRepository,
            realWorkingDayCountService,
          )
            .createReport(
              listOf(bed),
              BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

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
  ) {
    val application = expectedAssessment.application as TemporaryAccommodationApplicationEntity
    val isAssessmentRejected = REJECTED.name == expectedAssessment.decision?.name
    val rejectedDate = if (isAssessmentRejected) expectedAssessment.submittedAt else null
    val isReferralRejected =
      expectedAssessment.referralRejectionReason?.name == "They have no recourse to public funds (NRPF)" ||
        expectedAssessment.referralRejectionReason?.name == "They’re not eligible (not because of NRPF)"
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
    assertThat(actualReferralReportRow.referralRejected).isEqualTo(isReferralRejected.toYesNo())
    assertThat(actualReferralReportRow.rejectionDate).isEqualTo(rejectedDate?.toLocalDate())
    assertThat(actualReferralReportRow.rejectionReason).isEqualTo(expectedAssessment.rejectionRationale)
    assertThat(actualReferralReportRow.accommodationRequiredDate).isEqualTo(application.arrivalDate?.toLocalDate())
    assertThat(actualReferralReportRow.prisonAtReferral).isEqualTo(application.prisonNameOnCreation)
    assertThat(actualReferralReportRow.releaseDate).isEqualTo(application.personReleaseDate)
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
    }
  }
}
