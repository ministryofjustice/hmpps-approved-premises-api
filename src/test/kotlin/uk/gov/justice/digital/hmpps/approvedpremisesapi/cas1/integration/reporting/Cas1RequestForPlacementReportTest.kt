package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.reporting

import com.opencsv.CSVReaderBuilder
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1SimpleApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class Cas1RequestForPlacementReportTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1SimpleApiClient: Cas1SimpleApiClient

  lateinit var assessor: UserEntity
  lateinit var assessorJwt: String

  val standardRFPSubmittedNotAssessedAndWithdrawn = StandardRFPSubmittedNotAssessedAndWithdrawn()
  val standardRFPRejectedManager = StandardRFPRejectedManager()
  val standardRFPAcceptedAndWithdrawnManager = StandardRFPAcceptedAndWithdrawnManager()

  val standardRFPSubmittedBeforeReportingPeriod = StandardRFPSubmittedBeforeReportingPeriod()
  val standardRFPSubmittedAfterReportingPeriod = StandardRFPSubmittedAfterReportingPeriod()

  val placementAppRotlAcceptedManager = PlacementAppAssessedManager(
    type = PlacementType.rotl,
    submittedAt = LocalDateTime.of(2021, 3, 22, 9, 49, 0),
    withdraw = false,
  )
  val placementAppAdditionalAcceptedManager = PlacementAppAssessedManager(
    type = PlacementType.additionalPlacement,
    submittedAt = LocalDateTime.of(2021, 3, 23, 9, 49, 0),
    withdraw = false,
  )
  val placementAppParoleAcceptedAndWithdrawnManager = PlacementAppAssessedManager(
    type = PlacementType.releaseFollowingDecision,
    submittedAt = LocalDateTime.of(2021, 3, 24, 9, 49, 0),
    withdraw = true,
  )
  val placementAppRejectedManager = PlacementAppRejectedManager()

  val placementAppSubmittedBeforeReportingPeriod = PlacementAppSubmittedBeforeReportingPeriod()
  val placementAppSubmittedAfterReportingPeriod = PlacementAppSubmittedAfterReportingPeriod()

  @BeforeAll
  fun setup() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    val assessorDetails = givenAUser(
      roles = listOf(UserRole.CAS1_ASSESSOR),
      qualifications = UserQualification.entries,
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ASSESSOR1"),
    )
    assessor = assessorDetails.first
    assessorJwt = assessorDetails.second

    standardRFPSubmittedNotAssessedAndWithdrawn.createRequestForPlacement()
    standardRFPRejectedManager.createRequestForPlacement()
    standardRFPAcceptedAndWithdrawnManager.createRequestForPlacement()
    standardRFPSubmittedBeforeReportingPeriod.createRequestForPlacement()
    standardRFPSubmittedAfterReportingPeriod.createRequestForPlacement()

    placementAppRotlAcceptedManager.createRequestForPlacement()
    placementAppAdditionalAcceptedManager.createRequestForPlacement()
    placementAppParoleAcceptedAndWithdrawnManager.createRequestForPlacement()
    placementAppRejectedManager.createRequestForPlacement()
    placementAppSubmittedBeforeReportingPeriod.createRequestForPlacement()
    placementAppSubmittedAfterReportingPeriod.createRequestForPlacement()
  }

  @Test
  fun `Get application report returns OK with no applications`() {
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER))
    val startDate = LocalDate.of(2020, 2, 1)
    val endDate = LocalDate.of(2020, 2, 29)

    webTestClient.get()
      .uri(getReportUrl(Cas1ReportName.requestsForPlacement, startDate, endDate))
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"requests-for-placement-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"")
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readCSV(it.responseBody!!.inputStream())
          .convertTo<RequestForPlacementReportRow>(ExcessiveColumns.Remove)
          .toList()

        assertThat(actual.size).isEqualTo(0)
      }
  }

  @Test
  fun `Permission denied if trying to access report with PII without correct role`() {
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER))

    webTestClient.get()
      .uri(getReportUrl(Cas1ReportName.requestsForPlacementWithPii, startDate = LocalDate.now(), endDate = LocalDate.now()))
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get application report returns OK with applications, include PII`() {
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER_WITH_PII))
    val startDate = LocalDate.of(2021, 3, 1)
    val endDate = LocalDate.of(2021, 3, 31)

    webTestClient.get()
      .uri(getReportUrl(Cas1ReportName.requestsForPlacementWithPii, startDate, endDate))
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"requests-for-placement-with-pii-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"")
      .expectBody()
      .consumeWith { response ->
        val completeCsvString = response.responseBody!!.inputStream().bufferedReader().use { it.readText() }

        val csvReader = CSVReaderBuilder(StringReader(completeCsvString)).build()
        val headers = csvReader.readNext().toList()

        assertThat(headers).contains("referrer_username")
        assertThat(headers).contains("referrer_name")
        assertThat(headers).contains("applicant_reason_for_late_application_detail")
        assertThat(headers).contains("initial_assessor_reason_for_late_application")
        assertThat(headers).contains("initial_assessor_username")
        assertThat(headers).contains("initial_assessor_name")
        assertThat(headers).contains("last_appealed_assessor_username")

        val actual = DataFrame
          .readCSV(completeCsvString.byteInputStream())
          .convertTo<RequestForPlacementReportRow>(ExcessiveColumns.Remove)
          .toList()

        standardRFPAcceptedAndWithdrawnManager.assertRow(actual[0])
        standardRFPRejectedManager.assertRow(actual[1])
        standardRFPSubmittedNotAssessedAndWithdrawn.assertRow(actual[2])
        placementAppRotlAcceptedManager.assertRow(actual[3])
        placementAppAdditionalAcceptedManager.assertRow(actual[4])
        placementAppParoleAcceptedAndWithdrawnManager.assertRow(actual[5])
        placementAppRejectedManager.assertRow(actual[6])
      }
  }

  @Test
  fun `Get application report returns OK with applications, exclude PII by default and always exclude internal columns`() {
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER))
    val startDate = LocalDate.of(2021, 3, 1)
    val endDate = LocalDate.of(2021, 3, 31)

    webTestClient.get()
      .uri(getReportUrl(Cas1ReportName.requestsForPlacement, startDate, endDate))
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"requests-for-placement-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"")
      .expectBody()
      .consumeWith { response ->
        val completeCsvString = response.responseBody!!.inputStream().bufferedReader().use { it.readText() }

        val csvReader = CSVReaderBuilder(StringReader(completeCsvString)).build()
        val headers = csvReader.readNext().toList()

        assertThat(headers).doesNotContain("referrer_username")
        assertThat(headers).doesNotContain("referrer_name")
        assertThat(headers).doesNotContain("applicant_reason_for_late_application_detail")
        assertThat(headers).doesNotContain("initial_assessor_reason_for_late_application")
        assertThat(headers).doesNotContain("initial_assessor_username")
        assertThat(headers).doesNotContain("initial_assessor_name")
        assertThat(headers).doesNotContain("last_appealed_assessor_username")
        assertThat(headers).doesNotContain("internal_placement_request_id")
        assertThat(headers).doesNotContain("internal_placement_application_date_id")

        val actual = DataFrame
          .readCSV(completeCsvString.byteInputStream())
          .convertTo<RequestForPlacementReportRow>(ExcessiveColumns.Remove)
          .toList()

        assertThat(actual.size).isEqualTo(7)

        standardRFPAcceptedAndWithdrawnManager.assertRow(actual[0])
      }
  }

  inner class StandardRFPAcceptedAndWithdrawnManager {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createAndSubmitApplication(
        crn = "StandardRFPAcceptedAndWithdrawnManager",
        submittedAt = LocalDateTime.of(2019, 3, 15, 0, 10, 0),
        arrivalDateOnApplication = LocalDate.of(2021, 3, 12),
      )
      allocateAndUpdateLatestAssessment(
        applicationId = application.id,
        assessorJwt = assessorJwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        decisionDate = LocalDateTime.of(2020, 12, 1, 9, 15, 45),
        assessorJwt = assessorJwt,
        expectedArrival = LocalDate.of(2021, 3, 12),
        duration = 8,
      )
      withdrawPlacementRequest(
        applicationId = application.id,
        withdrawalDate = LocalDateTime.of(2021, 3, 15, 0, 10, 0),
        reason = WithdrawPlacementRequestReason.duplicatePlacementRequest,
      )
    }

    fun assertRow(row: RequestForPlacementReportRow) {
      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2021-03-12")
      assertThat(row.requested_duration_days).isEqualTo("8")
      assertThat(row.request_for_placement_submitted_date).isEqualTo("2019-03-15T00:10:00Z")
      assertThat(row.parole_decision_date).isNull()
      assertThat(row.request_for_placement_last_allocated_to_assessor_date).isEqualTo("2019-03-15T00:11:00Z")
      assertThat(row.request_for_placement_decision).isEqualTo("ACCEPTED")
      assertThat(row.request_for_placement_decision_made_date).isEqualTo("2020-12-01T09:15:45Z")
      assertThat(row.request_for_placement_withdrawal_date).isEqualTo("2021-03-15T00:10:00Z")
      assertThat(row.request_for_placement_withdrawal_reason).isEqualTo("DUPLICATE_PLACEMENT_REQUEST")
      assertThat(row.crn).isEqualTo("StandardRFPAcceptedAndWithdrawnManager")
    }
  }

  inner class StandardRFPRejectedManager {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createAndSubmitApplication(
        crn = "StandardRFPRejectedManager",
        submittedAt = LocalDateTime.of(2021, 3, 1, 12, 50, 0),
        arrivalDateOnApplication = LocalDate.of(2024, 1, 1),
      )
      allocateAndUpdateLatestAssessment(
        applicationId = application.id,
        assessorJwt = assessorJwt,
      )
      rejectLatestAssessment(
        applicationId = application.id,
        decisionDate = LocalDateTime.of(2020, 4, 1, 9, 15, 45),
        rationale = "the rejection rationale value",
        assessorJwt = assessorJwt,
      )
    }

    fun assertRow(row: RequestForPlacementReportRow) {
      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2024-01-01")
      assertThat(row.requested_duration_days).isNull()
      assertThat(row.request_for_placement_submitted_date).isEqualTo("2021-03-01T12:50:00Z")
      assertThat(row.parole_decision_date).isNull()
      assertThat(row.request_for_placement_last_allocated_to_assessor_date).isEqualTo("2021-03-01T12:51:00Z")
      assertThat(row.request_for_placement_decision).isEqualTo("REJECTED")
      assertThat(row.request_for_placement_decision_made_date).isEqualTo("2020-04-01T09:15:45Z")
      assertThat(row.request_for_placement_withdrawal_date).isNull()
      assertThat(row.request_for_placement_withdrawal_reason).isNull()
      assertThat(row.crn).isEqualTo("StandardRFPRejectedManager")
    }
  }

  inner class StandardRFPSubmittedNotAssessedAndWithdrawn {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createAndSubmitApplication(
        crn = "StandardRFPNotAssessed",
        submittedAt = LocalDateTime.of(2021, 3, 1, 15, 25, 5),
        arrivalDateOnApplication = LocalDate.of(2022, 12, 31),
      )

      withdrawApplication(
        applicationId = application.id,
        withdrawalDate = LocalDateTime.of(2022, 1, 12, 10, 15, 0),
        reason = WithdrawalReason.duplicateApplication,
      )
    }

    fun assertRow(row: RequestForPlacementReportRow) {
      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2022-12-31")
      assertThat(row.requested_duration_days).isNull()
      assertThat(row.request_for_placement_submitted_date).isEqualTo("2021-03-01T15:25:05Z")
      assertThat(row.parole_decision_date).isNull()
      assertThat(row.request_for_placement_last_allocated_to_assessor_date).isEqualTo("2021-03-01T15:25:05Z")
      assertThat(row.request_for_placement_decision).isNull()
      assertThat(row.request_for_placement_decision_made_date).isNull()
      assertThat(row.request_for_placement_withdrawal_date).isEqualTo("2022-01-12T10:15:00Z")
      assertThat(row.request_for_placement_withdrawal_reason).isEqualTo("duplicate_application")
      assertThat(row.crn).isEqualTo("StandardRFPNotAssessed")
    }
  }

  inner class StandardRFPSubmittedBeforeReportingPeriod {
    fun createRequestForPlacement() {
      createAndSubmitApplication(
        crn = "StandardRFPSubmittedBeforeReportingPeriod",
        submittedAt = LocalDateTime.of(2021, 2, 28, 23, 59, 59),
        arrivalDateOnApplication = LocalDate.of(2022, 12, 31),
      )
    }
  }

  inner class StandardRFPSubmittedAfterReportingPeriod {
    fun createRequestForPlacement() {
      createAndSubmitApplication(
        crn = "StandardRFPSubmittedAfterReportingPeriod",
        submittedAt = LocalDateTime.of(2021, 4, 1, 4, 0, 0),
        arrivalDateOnApplication = LocalDate.of(2022, 12, 31),
      )
    }
  }

  inner class PlacementAppAssessedManager(
    val type: PlacementType,
    val submittedAt: LocalDateTime,
    val withdraw: Boolean = false,
  ) {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var placementApplicationId: UUID

    fun createRequestForPlacement() {
      application = createAndSubmitApplication(
        crn = "${type}PlacementAppAssessed",
        submittedAt = LocalDateTime.of(2021, 2, 28, 23, 59, 59),
        arrivalDateOnApplication = null,
      )
      allocateAndUpdateLatestAssessment(
        applicationId = application.id,
        assessorJwt = assessorJwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        decisionDate = LocalDateTime.of(2020, 12, 1, 9, 15, 45),
        assessorJwt = assessorJwt,
        expectedArrival = LocalDate.of(2021, 3, 12),
        duration = 8,
      )
      createPlacementApplication(
        application = application,
        placementType = type,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(2029, 1, 1),
            duration = 25,
          ),
        ),
        paroleDecisionDate = LocalDate.of(2020, 2, 10),
        submittedAt = submittedAt,
      )
      placementApplicationId = decisionPlacementApplication(
        application = application,
        decisionMadeAt = LocalDateTime.of(2021, 3, 24, 15, 20, 0),
        decision = PlacementApplicationDecision.accepted,
      )
      if (withdraw) {
        withdrawPlacementApplication(
          placementApplicationId = placementApplicationId,
          withdrawalDate = LocalDateTime.of(2022, 5, 12, 10, 15, 0),
          reason = WithdrawPlacementRequestReason.changeInCircumstances,
        )
      }
    }

    fun assertRow(row: RequestForPlacementReportRow) {
      assertThat(row.request_for_placement_id).isEqualTo(placementApplicationId.toString())
      assertThat(row.request_for_placement_type).isEqualTo(
        when (type) {
          PlacementType.rotl -> "ROTL"
          PlacementType.releaseFollowingDecision -> "RELEASE_FOLLOWING_DECISION"
          PlacementType.additionalPlacement -> "ADDITIONAL_PLACEMENT"
        },
      )
      assertThat(row.requested_arrival_date).isEqualTo("2029-01-01")
      assertThat(row.requested_duration_days).isEqualTo("25")
      assertThat(row.request_for_placement_submitted_date).isEqualTo(submittedAt.format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'")))
      assertThat(row.parole_decision_date).isEqualTo("2020-02-10")
      assertThat(row.request_for_placement_decision).isEqualTo("ACCEPTED")
      assertThat(row.request_for_placement_decision_made_date).isEqualTo("2021-03-24T15:20:00Z")
      if (withdraw) {
        assertThat(row.request_for_placement_withdrawal_date).isEqualTo("2022-05-12T10:15:00Z")
        assertThat(row.request_for_placement_withdrawal_reason).isEqualTo("CHANGE_IN_CIRCUMSTANCES")
      } else {
        assertThat(row.request_for_placement_withdrawal_date).isNull()
        assertThat(row.request_for_placement_withdrawal_reason).isNull()
      }
      assertThat(row.crn).isEqualTo("${type}PlacementAppAssessed")
    }
  }

  inner class PlacementAppRejectedManager {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var placementApplicationId: UUID

    fun createRequestForPlacement() {
      application = createAndSubmitApplication(
        crn = "PlacementAppRejected",
        submittedAt = LocalDateTime.of(2021, 2, 28, 23, 59, 59),
        arrivalDateOnApplication = null,
      )
      allocateAndUpdateLatestAssessment(
        applicationId = application.id,
        assessorJwt = assessorJwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        decisionDate = LocalDateTime.of(2020, 12, 1, 9, 15, 45),
        assessorJwt = assessorJwt,
        expectedArrival = LocalDate.of(2021, 3, 12),
        duration = 8,
      )
      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(2029, 1, 1),
            duration = 25,
          ),
        ),
        paroleDecisionDate = LocalDate.of(2020, 2, 10),
        submittedAt = LocalDateTime.of(2021, 3, 29, 23, 59, 59),
      )
      placementApplicationId = decisionPlacementApplication(
        application = application,
        decisionMadeAt = LocalDateTime.of(2025, 12, 1, 15, 20, 0),
        decision = PlacementApplicationDecision.rejected,
      )
    }

    fun assertRow(row: RequestForPlacementReportRow) {
      assertThat(row.request_for_placement_id).isEqualTo(placementApplicationId.toString())
      assertThat(row.request_for_placement_type).isEqualTo("ROTL")
      assertThat(row.requested_arrival_date).isEqualTo("2029-01-01")
      assertThat(row.requested_duration_days).isEqualTo("25")
      assertThat(row.request_for_placement_submitted_date).isEqualTo("2021-03-29T23:59:59Z")
      assertThat(row.parole_decision_date).isEqualTo("2020-02-10")
      assertThat(row.request_for_placement_decision).isEqualTo("REJECTED")
      assertThat(row.request_for_placement_decision_made_date).isEqualTo("2025-12-01T15:20:00Z")
      assertThat(row.request_for_placement_withdrawal_date).isNull()
      assertThat(row.request_for_placement_withdrawal_reason).isNull()
      assertThat(row.crn).isEqualTo("PlacementAppRejected")
    }
  }

  inner class PlacementAppSubmittedBeforeReportingPeriod {
    fun createRequestForPlacement() {
      val application = createAndSubmitApplication(
        crn = "PlacementAppSubmittedBeforeReportingPeriod",
        submittedAt = LocalDateTime.of(2021, 2, 28, 23, 59, 59),
        arrivalDateOnApplication = null,
      )
      allocateAndUpdateLatestAssessment(
        applicationId = application.id,
        assessorJwt = assessorJwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        decisionDate = LocalDateTime.of(2020, 12, 1, 9, 15, 45),
        assessorJwt = assessorJwt,
        expectedArrival = LocalDate.of(2021, 3, 12),
        duration = 8,
      )
      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(2029, 1, 1),
            duration = 25,
          ),
        ),
        paroleDecisionDate = LocalDate.of(2020, 2, 10),
        submittedAt = LocalDateTime.of(2021, 2, 28, 22, 59, 59),
      )
    }
  }

  inner class PlacementAppSubmittedAfterReportingPeriod {
    fun createRequestForPlacement() {
      val application = createAndSubmitApplication(
        crn = "PlacementAppSubmittedAfterReportingPeriod",
        submittedAt = LocalDateTime.of(2021, 2, 28, 23, 59, 59),
        arrivalDateOnApplication = null,
      )
      allocateAndUpdateLatestAssessment(
        applicationId = application.id,
        assessorJwt = assessorJwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        decisionDate = LocalDateTime.of(2020, 12, 1, 9, 15, 45),
        assessorJwt = assessorJwt,
        expectedArrival = LocalDate.of(2021, 3, 12),
        duration = 8,
      )
      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(2029, 1, 1),
            duration = 25,
          ),
        ),
        paroleDecisionDate = LocalDate.of(2020, 2, 10),
        submittedAt = LocalDateTime.of(2021, 4, 1, 1, 0, 0),
      )
    }
  }

  @SuppressWarnings("ConstructorParameterNaming")
  data class RequestForPlacementReportRow(
    val request_for_placement_id: String?,
    val request_for_placement_type: String?,
    val requested_arrival_date: String?,
    val requested_duration_days: String?,
    val request_for_placement_submitted_date: String?,
    val parole_decision_date: String?,
    val request_for_placement_last_allocated_to_assessor_date: String?,
    val request_for_placement_decision: String?,
    val request_for_placement_decision_made_date: String?,
    val request_for_placement_withdrawal_date: String?,
    val request_for_placement_withdrawal_reason: String?,
    val crn: String?,
  )

  private fun createAndSubmitApplication(
    crn: String,
    submittedAt: LocalDateTime,
    arrivalDateOnApplication: LocalDate?,
  ): ApprovedPremisesApplicationEntity {
    val (applicant, jwt) = givenAUser()
    val (offenderDetails, _) = givenAnOffender(
      offenderDetailsConfigBlock = {
        withCrn(crn)
      },
    )

    apDeliusContextMockSuccessfulCaseDetailCall(
      crn,
      CaseDetailFactory().from(offenderDetails.asCaseDetail()).produce(),
    )

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(applicant)
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
      withData("{}")
      withOffenceId("offenceId")
      withRiskRatings(PersonRisksFactory().produce())
    }

    clock.setNow(submittedAt)

    cas1SimpleApiClient.applicationSubmit(
      this,
      application.id,
      jwt,
      SubmitApprovedPremisesApplication(
        arrivalDate = arrivalDateOnApplication,
        translatedDocument = {},
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "targetLocation",
        releaseType = ReleaseTypeOption.notApplicable,
        type = "CAS1",
        sentenceType = SentenceTypeOption.bailPlacement,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
        caseManagerIsNotApplicant = false,
        apType = ApType.pipe,
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
      ),
    )

    return realApplicationRepository.findByIdOrNull(application.id) as ApprovedPremisesApplicationEntity
  }

  private fun allocateAndUpdateLatestAssessment(
    applicationId: UUID,
    assessorJwt: String,
  ) {
    clock.advanceOneMinute()

    cas1SimpleApiClient.assessmentReallocate(
      this,
      getLatestAssessment(applicationId).id,
      assessor.id,
    )

    val assessmentId = getLatestAssessment(applicationId).id
    cas1SimpleApiClient.assessmentUpdate(
      this,
      assessmentId,
      assessorJwt,
      UpdateAssessment(data = mapOf("key" to "value")),
    )
  }

  private fun acceptLatestAssessment(
    applicationId: UUID,
    decisionDate: LocalDateTime,
    assessorJwt: String,
    expectedArrival: LocalDate,
    duration: Int,
  ) {
    val assessmentId = getLatestAssessment(applicationId).id

    val essentialCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isESAP)
    val desirableCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.acceptsSexOffenders)

    val placementRequirements = PlacementRequirements(
      type = ApType.normal,
      location = postCodeDistrictFactory.produceAndPersist().outcode,
      radius = 50,
      essentialCriteria = essentialCriteria,
      desirableCriteria = desirableCriteria,
    )

    clock.setNow(decisionDate)

    cas1SimpleApiClient.assessmentAccept(
      this,
      assessmentId,
      assessorJwt,
      AssessmentAcceptance(
        document = mapOf("document" to "value"),
        requirements = placementRequirements,
        placementDates =
        PlacementDates(
          expectedArrival = expectedArrival,
          duration = duration,
        ),
        apType = ApType.normal,
      ),
    )
  }

  private fun rejectLatestAssessment(
    applicationId: UUID,
    decisionDate: LocalDateTime,
    rationale: String,
    assessorJwt: String,
  ) {
    val assessmentId = getLatestAssessment(applicationId).id

    clock.setNow(decisionDate)

    cas1SimpleApiClient.assessmentReject(
      this,
      assessmentId,
      assessorJwt,
      AssessmentRejection(
        document = mapOf("document" to "value"),
        rejectionRationale = rationale,
      ),
    )
  }

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    placementType: PlacementType,
    placementDates: List<PlacementDates>,
    paroleDecisionDate: LocalDate,
    submittedAt: LocalDateTime,
  ) {
    val creatorJwt = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

    clock.setNow(submittedAt)

    cas1SimpleApiClient.placementApplicationCreate(
      this,
      creatorJwt = creatorJwt,
      NewPlacementApplication(application.id),
    )

    val placementApplicationId = getPlacementApplication(application).id

    cas1SimpleApiClient.placementApplicationUpdate(
      this,
      creatorJwt = creatorJwt,
      placementApplicationId = placementApplicationId,
      body = UpdatePlacementApplication(
        mapOf(
          "request-a-placement" to
            mapOf(
              "decision-to-release" to
                mapOf("decisionToReleaseDate" to paroleDecisionDate.toString()),
            ),
        ),
      ),
    )

    cas1SimpleApiClient.placementApplicationSubmit(
      this,
      creatorJwt = creatorJwt,
      placementApplicationId = placementApplicationId,
      body = SubmitPlacementApplication(
        translatedDocument = mapOf("key" to "value"),
        placementType = placementType,
        placementDates = placementDates,
        requestedPlacementPeriods = emptyList(),
      ),
    )
  }

  private fun decisionPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    decisionMadeAt: LocalDateTime,
    decision: PlacementApplicationDecision,
  ): UUID {
    clock.advanceOneMinute()

    cas1SimpleApiClient.placementApplicationReallocate(
      integrationTestBase = this,
      placementApplicationId = getPlacementApplication(application).id,
      NewReallocation(userId = assessor.id),
    )

    clock.setNow(decisionMadeAt)

    val latestPlacementApplicationId = getPlacementApplication(application).id

    cas1SimpleApiClient.placementApplicationDecision(
      integrationTestBase = this,
      placementApplicationId = latestPlacementApplicationId,
      assessorJwt = assessorJwt,
      body = PlacementApplicationDecisionEnvelope(
        decision = decision,
        summaryOfChanges = "summary",
        decisionSummary = "decisionSummary",
      ),
    )

    return latestPlacementApplicationId
  }

  private fun withdrawPlacementApplication(
    placementApplicationId: UUID,
    withdrawalDate: LocalDateTime,
    reason: WithdrawPlacementRequestReason,
  ) {
    clock.setNow(withdrawalDate)

    cas1SimpleApiClient.placementApplicationWithdraw(
      this,
      placementApplicationId,
      WithdrawPlacementApplication(
        reason,
      ),
    )
  }

  private fun withdrawPlacementRequest(
    applicationId: UUID,
    withdrawalDate: LocalDateTime,
    reason: WithdrawPlacementRequestReason,
  ) {
    clock.setNow(withdrawalDate)

    val placementRequestId = getApplication(applicationId).placementRequests.first { it.isForApplicationsArrivalDate() }.id

    cas1SimpleApiClient.placementRequestWithdraw(
      this,
      placementRequestId,
      WithdrawPlacementRequest(
        reason,
      ),
    )
  }

  private fun withdrawApplication(
    applicationId: UUID,
    withdrawalDate: LocalDateTime,
    reason: WithdrawalReason,
  ) {
    clock.setNow(withdrawalDate)

    cas1SimpleApiClient.applicationWithdraw(
      this,
      applicationId,
      NewWithdrawal(reason),
    )
  }

  private fun getPlacementApplication(application: ApplicationEntity) = placementApplicationRepository.findByApplication(application).first { it.reallocatedAt == null }

  private fun getLatestAssessment(applicationId: UUID) = getApplication(applicationId)
    .assessments.filter { it.reallocatedAt == null }.maxByOrNull { it.createdAt }!!

  private fun getApplication(applicationId: UUID) = realApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

  private fun getReportUrl(reportName: Cas1ReportName, startDate: LocalDate, endDate: LocalDate) = "/cas1/reports/${reportName.value}?startDate=$startDate&endDate=$endDate"
}
