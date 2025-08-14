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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.reporting.Cas1PlacementMatchingOutcomesV2ReportTest.Constants.REPORT_MONTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.reporting.Cas1PlacementMatchingOutcomesV2ReportTest.Constants.REPORT_YEAR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1SimpleApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Cas1PlacementMatchingOutcomesV2ReportTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1SimpleApiClient: Cas1SimpleApiClient

  lateinit var assessor: UserEntity
  lateinit var assessorJwt: String

  val standardRFPNoDecision = StandardRFPNoDecision()
  val standardRFPMatched = StandardRFPMatched()
  val standardRFPNotMatchedAndWithdrawn = StandardRFPNotMatchedAndWithdrawn()
  val standardRFPNoDecisionBeforeReportingMonth = StandardRFPNoDecisionBeforeReportingMonth()
  val standardRFPNoDecisionAfterReportingMonth = StandardRFPNoDecisionAfterReportingMonth()
  val standardRFPNotMatchedAndThenMatched = StandardRFPNotMatchedAndThenMatched()
  val standardRFPMultipleTransfers = StandardRFPMultipleTransfers()
  val placementAppMatched = PlacementAppMatched()
  val placementAppBeforeReportingMonth = PlacementAppBeforeReportingMonth()
  val placementAppAfterReportingMonth = PlacementAppAfterReportingMonth()

  object Constants {
    const val REPORT_MONTH = 2
    const val REPORT_YEAR = 2020
  }

  @BeforeAll
  fun setup() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    val assessorDetails = givenAUser(
      roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_CRU_MEMBER),
      qualifications = UserQualification.entries,
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ASSESSOR1"),
    )
    assessor = assessorDetails.first
    assessorJwt = assessorDetails.second

    standardRFPNoDecision.createRequestForPlacement()
    standardRFPMatched.createRequestForPlacement()
    standardRFPNotMatchedAndWithdrawn.createRequestForPlacement()
    standardRFPNoDecisionBeforeReportingMonth.createRequestForPlacement()
    standardRFPNoDecisionAfterReportingMonth.createRequestForPlacement()
    standardRFPNotMatchedAndThenMatched.createRequestForPlacement()
    standardRFPMultipleTransfers.createRequestForPlacement()
    placementAppMatched.createRequestForPlacement()
    placementAppBeforeReportingMonth.createRequestForPlacement()
    placementAppAfterReportingMonth.createRequestForPlacement()
  }

  @Test
  fun `Get report is empty if no applications`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      val startDate = LocalDate.of(2019, 2, 1)
      val endDate = LocalDate.of(2019, 2, 28)

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementMatchingOutcomesV2, startDate, endDate))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placement-matching-outcomes-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<PlacementMatchingOutcomeReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(0)
        }
    }
  }

  @Test
  fun `Get report returns OK with correct applications, excluding PII`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      val startDate = LocalDate.of(2020, 2, 1)
      val endDate = LocalDate.of(2020, 2, 29)

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementMatchingOutcomesV2, startDate, endDate))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placement-matching-outcomes-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"")
        .expectBody()
        .consumeWith { response ->
          val completeCsvString = response.responseBody!!.inputStream().bufferedReader().use { it.readText() }

          val csvReader = CSVReaderBuilder(StringReader(completeCsvString)).build()
          val headers = csvReader.readNext().toList()

          assertThat(headers).doesNotContain("matcher_username")
          assertThat(headers).doesNotContain("matcher_name")

          val actual = DataFrame
            .readCSV(completeCsvString.byteInputStream())
            .convertTo<PlacementMatchingOutcomeReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(6)

          standardRFPNoDecision.assertRow(actual[0])
        }
    }
  }

  @Test
  fun `Permission denied if trying to access report with PII without correct role`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementMatchingOutcomesV2WithPii, startDate = LocalDate.of(2020, 2, 1), endDate = LocalDate.of(2020, 2, 29)))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get report returns OK with correct applications, including PII`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER_WITH_PII)) { _, jwt ->
      val startDate = LocalDate.of(2020, 2, 1)
      val endDate = LocalDate.of(2020, 2, 29)

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementMatchingOutcomesV2WithPii, startDate, endDate))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placement-matching-outcomes-with-pii-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<PlacementMatchingOutcomeReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(6)

          standardRFPNoDecision.assertRow(actual[0])
          standardRFPMatched.assertRow(actual[1])
          standardRFPNotMatchedAndWithdrawn.assertRow(actual[2])
          standardRFPNotMatchedAndThenMatched.assertRow(actual[3])
          standardRFPMultipleTransfers.assertRow(actual[4])
          placementAppMatched.assertRow(actual[5])
        }
    }
  }

  inner class StandardRFPNoDecision {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "StandardRFPNotAllocated",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
      )
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.placement_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isNull()
      assertThat(row.matcher_username).isNull()
      assertThat(row.matcher_name).isNull()
      assertThat(row.match_outcome).isNull()

      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.crn).matches("StandardRFPNotAllocated")

      assertThat(row.first_match_attempt_date_time).isNull()
      assertThat(row.first_successful_match_attempt_date_time).isNull()
      assertThat(row.first_successful_match_attempt_premises_name).isNull()
      assertThat(row.last_successful_match_attempt_date_time).isNull()
      assertThat(row.last_successful_match_attempt_premises_name).isNull()
      assertThat(row.last_successful_match_placement_id).isNull()
    }
  }

  inner class StandardRFPNoDecisionBeforeReportingMonth {
    fun createRequestForPlacement() {
      createSubmitAndAssessedApplication(
        crn = "StandardRFPNoDecisionBeforeReportingMonth",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH - 1, 31),
      )
    }
  }

  inner class StandardRFPNoDecisionAfterReportingMonth {
    fun createRequestForPlacement() {
      createSubmitAndAssessedApplication(
        crn = "StandardRFPNoDecisionAfterReportingMonth",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH + 1, 2),
      )
    }
  }

  inner class StandardRFPMatched {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var latestBookingId: UUID

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "StandardRFPMatched",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 2),
      )

      clock.setNow(LocalDateTime.of(2030, 1, 2, 3, 4, 5))

      latestBookingId = createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER1",
        matcherApAreaName = "MATCHER1CRU",
        premisesName = "StandardRFPMatched Premise",
      )
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.placement_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isEqualTo("MATCHER1CRU")
      assertThat(row.matcher_username).isEqualTo("MATCHER1")
      assertThat(row.matcher_name).isEqualTo("Jeff Jefferson")
      assertThat(row.match_outcome).isEqualTo("Placed")

      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2020-02-02")
      assertThat(row.crn).matches("StandardRFPMatched")

      assertThat(row.first_match_attempt_date_time).isEqualTo("2030-01-02T03:04:05Z")
      assertThat(row.first_successful_match_attempt_date_time).isEqualTo("2030-01-02T03:04:05Z")
      assertThat(row.first_successful_match_attempt_premises_name).isEqualTo("StandardRFPMatched Premise")
      assertThat(row.last_successful_match_attempt_date_time).isEqualTo("2030-01-02T03:04:05Z")
      assertThat(row.last_successful_match_attempt_premises_name).isEqualTo("StandardRFPMatched Premise")
      assertThat(row.last_successful_match_placement_id).isEqualTo(latestBookingId.toString())
    }
  }

  inner class StandardRFPNotMatchedAndWithdrawn {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "StandardRFPNotMatchedAndWithdrawn",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 3),
      )

      clock.setNow(LocalDateTime.of(2028, 1, 2, 3, 4, 5))

      createBookingNotMade(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER2",
        matcherApAreaName = "MATCHER2CRU",
      )

      withdrawPlacementRequest(applicationId = application.id)
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.placement_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isEqualTo("MATCHER2CRU")
      assertThat(row.matcher_username).isEqualTo("MATCHER2")
      assertThat(row.match_outcome).isEqualTo("Not matched")

      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2020-02-03")
      assertThat(row.crn).matches("StandardRFPNotMatchedAndWithdrawn")

      assertThat(row.first_match_attempt_date_time).isEqualTo("2028-01-02T03:04:05Z")
      assertThat(row.first_successful_match_attempt_date_time).isNull()
      assertThat(row.first_successful_match_attempt_premises_name).isNull()
      assertThat(row.last_successful_match_attempt_date_time).isNull()
      assertThat(row.last_successful_match_attempt_premises_name).isNull()
    }
  }

  inner class StandardRFPNotMatchedAndThenMatched {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var latestBookingId: UUID

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "StandardRFPMNotMatchedAndThenMatched",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 4),
      )
      clock.setNow(LocalDateTime.of(2028, 2, 3, 4, 5, 7))

      createBookingNotMade(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER3",
        matcherApAreaName = "MATCHER3CRU",
      )

      clock.setNow(LocalDateTime.of(2030, 2, 3, 4, 5, 6))

      latestBookingId = createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER13",
        matcherApAreaName = "MATCHER13CRU",
        premisesName = "StandardRFPNotMatchedAndThenMatched premise",
      )
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.placement_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isEqualTo("MATCHER13CRU")
      assertThat(row.matcher_username).isEqualTo("MATCHER13")
      assertThat(row.matcher_name).isEqualTo("Jeff Jefferson")
      assertThat(row.match_outcome).isEqualTo("Placed")

      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2020-02-04")
      assertThat(row.crn).matches("StandardRFPMNotMatchedAndThenMatched")

      assertThat(row.first_match_attempt_date_time).isEqualTo("2028-02-03T04:05:07Z")
      assertThat(row.first_successful_match_attempt_date_time).isEqualTo("2030-02-03T04:05:06Z")
      assertThat(row.first_successful_match_attempt_premises_name).isEqualTo("StandardRFPNotMatchedAndThenMatched premise")
      assertThat(row.last_successful_match_attempt_date_time).isEqualTo("2030-02-03T04:05:06Z")
      assertThat(row.last_successful_match_attempt_premises_name).isEqualTo("StandardRFPNotMatchedAndThenMatched premise")
      assertThat(row.last_successful_match_placement_id).isEqualTo(latestBookingId.toString())
    }
  }

  inner class StandardRFPMultipleTransfers {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var latestBookingId: UUID

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "StandardRFPMultipleTransfers",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 5),
      )
      clock.setNow(LocalDateTime.of(2028, 2, 3, 4, 5, 7))

      createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER4",
        matcherApAreaName = "MATCHER4CRU",
        premisesName = "StandardRFPMultipleTransfers premise 1",
      )

      cancelBooking(application.id)

      clock.setNow(LocalDateTime.of(2030, 2, 3, 4, 5, 6))

      createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER5",
        matcherApAreaName = "MATCHER5CRU",
        premisesName = "StandardRFPMultipleTransfers premise 2",
      )

      cancelBooking(application.id)

      clock.setNow(LocalDateTime.of(2032, 1, 2, 3, 4, 5))

      latestBookingId = createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER6",
        matcherApAreaName = "MATCHER6CRU",
        premisesName = "StandardRFPMultipleTransfers premise 3",
      )
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.placement_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isEqualTo("MATCHER6CRU")
      assertThat(row.matcher_username).isEqualTo("MATCHER6")
      assertThat(row.match_outcome).isEqualTo("Placed")

      assertThat(row.request_for_placement_id).matches("[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.requested_arrival_date).isEqualTo("2020-02-05")
      assertThat(row.crn).matches("StandardRFPMultipleTransfers")

      assertThat(row.first_match_attempt_date_time).isEqualTo("2028-02-03T04:05:07Z")
      assertThat(row.first_successful_match_attempt_date_time).isEqualTo("2028-02-03T04:05:07Z")
      assertThat(row.first_successful_match_attempt_premises_name).isEqualTo("StandardRFPMultipleTransfers premise 1")
      assertThat(row.last_successful_match_attempt_date_time).isEqualTo("2032-01-02T03:04:05Z")
      assertThat(row.last_successful_match_attempt_premises_name).isEqualTo("StandardRFPMultipleTransfers premise 3")
      assertThat(row.last_successful_match_placement_id).isEqualTo(latestBookingId.toString())
    }
  }

  inner class PlacementAppMatched {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var latestBookingId: UUID
    lateinit var placementApplicationId: UUID

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "ROTLRFPMultiDates",
        arrivalDateOnApplication = null,
      )

      placementApplicationId = createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 6),
            duration = 5,
          ),
        ),
      )

      clock.setNow(LocalDateTime.of(2031, 8, 7, 6, 5, 6))

      latestBookingId = createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER10",
        matcherApAreaName = "MATCHER10CRU",
        premisesName = "PlacementAppMatched Premise",
      )
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.placement_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isEqualTo("MATCHER10CRU")
      assertThat(row.matcher_username).isEqualTo("MATCHER10")
      assertThat(row.match_outcome).isEqualTo("Placed")

      assertThat(row.request_for_placement_id).isEqualTo(placementApplicationId.toString())
      assertThat(row.request_for_placement_type).isEqualTo("ROTL")
      assertThat(row.requested_arrival_date).isEqualTo("2020-02-06")
      assertThat(row.crn).matches("ROTLRFPMultiDates")

      assertThat(row.first_match_attempt_date_time).isEqualTo("2031-08-07T06:05:06Z")
      assertThat(row.first_successful_match_attempt_date_time).isEqualTo("2031-08-07T06:05:06Z")
      assertThat(row.first_successful_match_attempt_premises_name).isEqualTo("PlacementAppMatched Premise")
      assertThat(row.last_successful_match_attempt_date_time).isEqualTo("2031-08-07T06:05:06Z")
      assertThat(row.last_successful_match_attempt_premises_name).isEqualTo("PlacementAppMatched Premise")
      assertThat(row.last_successful_match_placement_id).isEqualTo(latestBookingId.toString())
    }
  }

  inner class PlacementAppBeforeReportingMonth {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "ROTLRFPMatched",
        arrivalDateOnApplication = null,
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH - 1, 31),
            duration = 5,
          ),
        ),
      )

      createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER11",
        matcherApAreaName = "MATCHER11CRU",
      )
    }
  }

  inner class PlacementAppAfterReportingMonth {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "ROTLRFPMatched",
        arrivalDateOnApplication = null,
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH + 1, 2),
            duration = 5,
          ),
        ),
      )

      createBooking(
        placementRequestId = application.placementRequests[0].id,
        matcherUsername = "MATCHER12",
        matcherApAreaName = "MATCHER12CRU",
      )
    }
  }

  private fun createBooking(
    placementRequestId: UUID,
    matcherUsername: String,
    matcherApAreaName: String,
    premisesName: String = randomStringMultiCaseWithNumbers(8),
  ): UUID {
    val managerJwt = givenAUser(
      roles = listOf(UserRole.CAS1_CRU_MEMBER),
      staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "Jeff", surname = "Jefferson"), deliusUsername = matcherUsername),
      probationRegion = givenAProbationRegion(apArea = givenAnApArea(name = matcherApAreaName)),
    ).second

    val premises = givenAnApprovedPremises(
      name = premisesName,
      supportsSpaceBookings = true,
    )

    return cas1SimpleApiClient.spaceBookingForPlacementRequest(
      integrationTestBase = this,
      placementRequestId = placementRequestId,
      managerJwt = managerJwt,
      Cas1NewSpaceBooking(
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        premisesId = premises.id,
        characteristics = emptyList(),
      ),
    ).id
  }

  private fun createBookingNotMade(
    placementRequestId: UUID,
    matcherApAreaName: String,
    matcherUsername: String,
  ) {
    val cruMemberJwt = givenAUser(
      roles = listOf(UserRole.CAS1_CRU_MEMBER),
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = matcherUsername),
      probationRegion = givenAProbationRegion(apArea = givenAnApArea(name = matcherApAreaName)),
    ).second

    cas1SimpleApiClient.placementRequestBookingNotMade(
      this,
      placementRequestId,
      cruMemberJwt,
      NewBookingNotMade(notes = "not this time"),
    )
  }

  private fun createSubmitAndAssessedApplication(
    crn: String,
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

    allocateAndUpdateLatestAssessment(applicationId = application.id)
    acceptLatestAssessment(
      applicationId = application.id,
      expectedArrival = arrivalDateOnApplication,
      duration = 8,
    )

    return realApplicationRepository.findByIdOrNull(application.id) as ApprovedPremisesApplicationEntity
  }

  private fun allocateAndUpdateLatestAssessment(applicationId: UUID) {
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
    expectedArrival: LocalDate?,
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

    cas1SimpleApiClient.assessmentAccept(
      this,
      assessmentId,
      assessorJwt,
      AssessmentAcceptance(
        document = mapOf("document" to "value"),
        requirements = placementRequirements,
        placementDates = expectedArrival?.let {
          PlacementDates(
            expectedArrival = it,
            duration = duration,
          )
        },
        apType = ApType.normal,
      ),
    )
  }

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    placementType: PlacementType,
    placementDates: List<PlacementDates>,
  ): UUID {
    val creatorJwt = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

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
        mapOf("doesnt" to "matter"),
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
        releaseType = ReleaseTypeOption.licence,
        sentenceType = null,
        situationType = null,
      ),
    )

    cas1SimpleApiClient.placementApplicationReallocate(
      integrationTestBase = this,
      placementApplicationId = getPlacementApplication(application).id,
      NewReallocation(userId = assessor.id),
    )

    val latestPlacementApplicationId = getPlacementApplication(application).id

    cas1SimpleApiClient.placementApplicationDecision(
      integrationTestBase = this,
      placementApplicationId = latestPlacementApplicationId,
      assessorJwt = assessorJwt,
      body = PlacementApplicationDecisionEnvelope(
        decision = PlacementApplicationDecision.accepted,
        summaryOfChanges = "summary",
        decisionSummary = "decisionSummary",
      ),
    )

    return latestPlacementApplicationId
  }

  private fun withdrawPlacementRequest(
    applicationId: UUID,
  ) {
    val placementRequestId = getApplication(applicationId).placementRequests.first { it.isForApplicationsArrivalDate() }.id

    cas1SimpleApiClient.placementRequestWithdraw(
      this,
      placementRequestId,
      WithdrawPlacementRequest(
        WithdrawPlacementRequestReason.duplicatePlacementRequest,
      ),
    )
  }

  private fun cancelBooking(
    applicationId: UUID,
  ) {
    val placementRequest = getApplication(applicationId).placementRequests.first { it.isForApplicationsArrivalDate() }
    val booking = placementRequest.spaceBookings.first { it.isActive() }

    cas1SimpleApiClient.spaceBookingCancel(
      integrationTestBase = this,
      premisesId = booking.premises.id,
      bookingId = booking.id,
    )
  }

  private fun getLatestAssessment(applicationId: UUID) = getApplication(applicationId)
    .assessments.filter { it.reallocatedAt == null }.maxByOrNull { it.createdAt }!!

  private fun getApplication(applicationId: UUID) = realApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

  private fun getPlacementApplications(application: ApplicationEntity) = placementApplicationRepository.findByApplication(application).filter { it.reallocatedAt == null }

  private fun getPlacementApplication(application: ApplicationEntity) = getPlacementApplications(application).first()

  private fun getReportUrl(reportName: Cas1ReportName, startDate: LocalDate, endDate: LocalDate) = "/cas1/reports/${reportName.value}?startDate=$startDate&endDate=$endDate"

  @SuppressWarnings("ConstructorParameterNaming")
  data class PlacementMatchingOutcomeReportRow(
    val placement_request_id: String?,
    val matcher_cru: String?,
    val matcher_username: String?,
    val matcher_name: String?,
    val match_outcome: String?,
    val crn: String?,
    val request_for_placement_id: String?,
    val request_for_placement_type: String?,
    val requested_arrival_date: String?,
    val first_match_attempt_date_time: String?,
    val first_successful_match_attempt_date_time: String?,
    val first_successful_match_attempt_premises_name: String?,
    val last_successful_match_attempt_date_time: String?,
    val last_successful_match_attempt_premises_name: String?,
    val last_successful_match_placement_id: String?,
  )
}
