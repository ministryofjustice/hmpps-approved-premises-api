package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1SimpleApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting.Cas1PlacementReportTest.Constants.REPORT_MONTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting.Cas1PlacementReportTest.Constants.REPORT_YEAR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.collections.toList

class Cas1PlacementReportTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1SimpleApiClient: Cas1SimpleApiClient

  lateinit var assessor: UserEntity
  lateinit var assessorJwt: String
  lateinit var user: UserEntity
  lateinit var criteria: MutableList<CharacteristicEntity>
  lateinit var premises: ApprovedPremisesEntity

  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity

  val bookingWithCanonicalArrivalDateWithinRange = BookingWithCanonicalArrivalDateWithinRange()
  val bookingWithCanonicalDepartureDateWithinRange = BookingWithCanonicalDepartureDateWithinRange()
  val bookingWithNonArrivalConfirmedDateWithinRange = BookingWithNonArrivalConfirmedDateWithinRange()
  val bookingWithCancellationDateWithinRange = BookingWithCancellationDateWithinRange()
  val bookingsOutsideReportingRange = BookingsOutsideReportingRange()

  object Constants {
    const val REPORT_MONTH = 2
    const val REPORT_YEAR = 2024
  }

  @BeforeAll
  fun setup() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
    characteristicRepository.deleteAll()

    val assessorDetails = givenAUser(
      roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER),
      qualifications = UserQualification.entries,
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ASSESSOR1"),
    )
    assessor = assessorDetails.first
    assessorJwt = assessorDetails.second

    user = givenAUser(
      roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA),
    ).first

    criteria = mutableListOf(
      characteristicEntityFactory.produceAndPersist {
        withName("Step Free Designated")
        withPropertyName("isStepFreeDesignated")
        withServiceScope("approved-premises")
        withModelScope("room")
      },
      characteristicEntityFactory.produceAndPersist {
        withName("Catered")
        withPropertyName("isCatered")
        withServiceScope("approved-premises")
        withModelScope("premises")
      },
    )

    premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }

      withYieldedProbationRegion { givenAProbationRegion(name = "southWest", apArea = givenAnApArea(name = "matcherApAreaName")) }
      withName("premisesName")
      withSupportsSpaceBookings(true)
    }

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist { withDefaults() }
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist { withDefaults() }

    bookingWithCanonicalArrivalDateWithinRange.createBooking()
    bookingWithCanonicalDepartureDateWithinRange.createBooking()
    bookingWithNonArrivalConfirmedDateWithinRange.createBooking()
    bookingWithCancellationDateWithinRange.createBooking()
    bookingsOutsideReportingRange.createOutOfRangeBookings(LocalDate.of(2024, 1, 30), LocalDate.of(2024, 3, 2))
  }

  @Test
  fun `Get report is empty if no space bookings`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placements, year = REPORT_YEAR - 1, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placements-2023-02-[0-9_]*.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<PlacementReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(0)
        }
    }
  }

  @Test
  fun `Permission denied if trying to access report with PII without correct role`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementsWithPii, year = REPORT_YEAR, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get report returns OK with correct applications, excluding PII`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placements, year = REPORT_YEAR, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placements-2024-02-[0-9_]*.csv\"")
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

          val actual = DataFrame
            .readCSV(completeCsvString.byteInputStream())
            .convertTo<PlacementReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(4)

          bookingWithCanonicalDepartureDateWithinRange.assertRow(actual[0])
          bookingWithNonArrivalConfirmedDateWithinRange.assertRow(actual[1])
          bookingWithCanonicalArrivalDateWithinRange.assertRow(actual[2])
          bookingWithCancellationDateWithinRange.assertRow(actual[3])
        }
    }
  }

  @Test
  fun `Get report returns OK with correct applications, including PII`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER_WITH_PII)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementsWithPii, year = REPORT_YEAR, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placements-with-pii-2024-02-[0-9_]*.csv\"")
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
            .convertTo<PlacementReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(4)

          bookingWithCanonicalDepartureDateWithinRange.assertRow(actual[0])
          bookingWithNonArrivalConfirmedDateWithinRange.assertRow(actual[1])
          bookingWithCanonicalArrivalDateWithinRange.assertRow(actual[2])
          bookingWithCancellationDateWithinRange.assertRow(actual[3])
        }
    }
  }

  inner class BookingWithCanonicalArrivalDateWithinRange {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var booking: Cas1SpaceBookingEntity

    fun createBooking() {
      application = createSubmitAndAssessedApplication(
        crn = "CRNTest1",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 5),
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.additionalPlacement,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 5),
            duration = 5,
          ),
        ),
      )

      booking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("CRN")
        withPremises(premises)
        withPlacementRequest(application.placementRequests[0])
        withApplication(application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.of(REPORT_YEAR, REPORT_MONTH, 7))
        withCanonicalDepartureDate(LocalDate.of(2024, 5, 10))
        withActualArrivalDate(LocalDate.of(2024, 2, 7))
        withActualArrivalTime(LocalTime.of(11, 0, 2, 0))
        withActualDepartureDate(LocalDate.of(2024, 5, 10))
        withActualDepartureTime(LocalTime.of(23, 0, 0, 0))
        withExpectedArrivalDate(LocalDate.of(2024, 2, 7))
        withExpectedDepartureDate(LocalDate.of(2024, 5, 10))
        withDepartureReason(null)
        withDepartureNotes(null)
        withMoveOnCategory(null)
        withCriteria(criteria)
        withNonArrivalReason(null)
        withNonArrivalNotes(null)
      }
    }

    fun assertRow(row: PlacementReportRow) {
      assertThat(row.placement_id).isEqualTo(booking.id.toString())
      assertThat(row.expected_arrival_date).isEqualTo("2024-02-07")
      assertThat(row.expected_departure_date).isEqualTo("2024-05-10")
      assertThat(row.actual_arrival_date).isEqualTo("2024-02-07")
      assertThat(row.actual_arrival_time).isEqualTo("11:00:02")
      assertThat(row.actual_departure_date).isEqualTo("2024-05-10")
      assertThat(row.actual_departure_time).isEqualTo("23:00")
      assertThat(row.premises_name).isEqualTo("premisesName")
      assertThat(row.premises_region).isEqualTo("southWest")
      assertThat(row.actual_duration_nights).isEqualTo("93")
      assertThat(row.expected_duration_nights).isEqualTo("93")
      assertThat(row.criteria?.split(", ")).containsExactlyInAnyOrder("isCatered", "isStepFreeDesignated")
      assertThat(row.departure_move_on_category).isNull()
      assertThat(row.departure_reason).isNull()
      assertThat(row.non_arrival_reason).isNull()
      assertThat(row.non_arrival_recorded_date_time).isNull()
      assertThat(row.placement_withdrawn_date).isNull()
      assertThat(row.placement_withdrawal_recorded_date_time).isNull()
      assertThat(row.placement_withdrawn_reason).isNull()
    }
  }

  inner class BookingWithCanonicalDepartureDateWithinRange {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var booking: Cas1SpaceBookingEntity
    lateinit var moveOnCategory: MoveOnCategoryEntity
    lateinit var departureReason: DepartureReasonEntity

    fun createBooking() {
      application = createSubmitAndAssessedApplication(
        crn = "CRNTest2",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15),
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15),
            duration = 5,
          ),
        ),
      )

      moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
        withName("Move on category")
      }

      departureReason = departureReasonEntityFactory.produceAndPersist {
        withName("Departed")
      }

      booking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("CRNTest2")
        withPremises(premises)
        withPlacementRequest(application.placementRequests[1])
        withApplication(application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.of(2024, 1, 7))
        withCanonicalDepartureDate(LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1))
        withActualArrivalDate(LocalDate.of(2024, 1, 7))
        withActualArrivalTime(LocalTime.of(11, 0, 2, 0))
        withActualDepartureDate(LocalDate.of(2024, 2, 1))
        withActualDepartureTime(LocalTime.of(23, 0, 0, 0))
        withExpectedArrivalDate(LocalDate.of(2024, 1, 7))
        withExpectedDepartureDate(LocalDate.of(2024, 2, 1))
        withDepartureReason(departureReason)
        withDepartureNotes(null)
        withMoveOnCategory(moveOnCategory)
        withCriteria(criteria)
        withNonArrivalReason(null)
        withNonArrivalNotes(null)
      }
    }

    fun assertRow(row: PlacementReportRow) {
      assertThat(row.placement_id).isEqualTo(booking.id.toString())
      assertThat(row.expected_arrival_date).isEqualTo("2024-01-07")
      assertThat(row.expected_departure_date).isEqualTo("2024-02-01")
      assertThat(row.actual_arrival_date).isEqualTo("2024-01-07")
      assertThat(row.actual_arrival_time).isEqualTo("11:00:02")
      assertThat(row.actual_departure_date).isEqualTo("2024-02-01")
      assertThat(row.actual_departure_time).isEqualTo("23:00")
      assertThat(row.premises_name).isEqualTo("premisesName")
      assertThat(row.premises_region).isEqualTo("southWest")
      assertThat(row.actual_duration_nights).isEqualTo("25")
      assertThat(row.expected_duration_nights).isEqualTo("25")
      assertThat(row.criteria?.split(", ")).containsExactlyInAnyOrder("isCatered", "isStepFreeDesignated")
      assertThat(row.departure_reason).isEqualTo("Departed")
      assertThat(row.departure_move_on_category).isEqualTo("Move on category")
      assertThat(row.non_arrival_reason).isNull()
      assertThat(row.non_arrival_recorded_date_time).isNull()
      assertThat(row.placement_withdrawn_date).isNull()
      assertThat(row.placement_withdrawal_recorded_date_time).isNull()
      assertThat(row.placement_withdrawn_reason).isNull()
    }
  }

  inner class BookingWithNonArrivalConfirmedDateWithinRange {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var booking: Cas1SpaceBookingEntity
    lateinit var nonArrivalReason: NonArrivalReasonEntity

    fun createBooking() {
      application = createSubmitAndAssessedApplication(
        crn = "CRNTest2",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15),
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15),
            duration = 5,
          ),
        ),
      )

      nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

      booking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("CRNTest2")
        withPremises(premises)
        withPlacementRequest(application.placementRequests[1])
        withApplication(application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.of(2024, 1, 15))
        withCanonicalDepartureDate(LocalDate.of(2024, 5, 1))
        withActualArrivalDate(null)
        withActualArrivalTime(null)
        withActualDepartureDate(null)
        withActualDepartureTime(null)
        withExpectedArrivalDate(LocalDate.of(2024, 1, 15))
        withExpectedDepartureDate(LocalDate.of(2024, 5, 1))
        withNonArrivalConfirmedAt(LocalDate.of(REPORT_YEAR, REPORT_MONTH, 2).atStartOfDay(ZoneId.systemDefault()).toInstant())
        withDepartureReason(null)
        withDepartureNotes(null)
        withMoveOnCategory(null)
        withCriteria(criteria)
        withNonArrivalReason(nonArrivalReason)
        withNonArrivalNotes(null)
      }
    }

    fun assertRow(row: PlacementReportRow) {
      assertThat(row.placement_id).isEqualTo(booking.id.toString())
      assertThat(row.expected_arrival_date).isEqualTo("2024-01-15")
      assertThat(row.expected_departure_date).isEqualTo("2024-05-01")
      assertThat(row.actual_arrival_date).isEqualTo(null)
      assertThat(row.actual_arrival_time).isEqualTo(null)
      assertThat(row.actual_departure_date).isEqualTo(null)
      assertThat(row.actual_departure_time).isEqualTo(null)
      assertThat(row.premises_name).isEqualTo("premisesName")
      assertThat(row.premises_region).isEqualTo("southWest")
      assertThat(row.actual_duration_nights).isNull()
      assertThat(row.expected_duration_nights).isEqualTo("107")
      assertThat(row.criteria?.split(", ")).containsExactlyInAnyOrder("isCatered", "isStepFreeDesignated")
      assertThat(row.departure_move_on_category).isNull()
      assertThat(row.departure_reason).isNull()
      assertThat(row.non_arrival_reason).isEqualTo(nonArrivalReason.name)
      assertThat(row.non_arrival_recorded_date_time).isEqualTo("2024-02-02T00:00:00Z")
      assertThat(row.placement_withdrawn_date).isNull()
      assertThat(row.placement_withdrawal_recorded_date_time).isNull()
      assertThat(row.placement_withdrawn_reason).isNull()
    }
  }

  inner class BookingWithCancellationDateWithinRange {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var booking: Cas1SpaceBookingEntity
    lateinit var cancellationReason: CancellationReasonEntity

    fun createBooking() {
      application = createSubmitAndAssessedApplication(
        crn = "CRNTest2",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15),
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15),
            duration = 5,
          ),
        ),
      )

      cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withName("Cancelled")
      }

      booking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("CRNTest2")
        withPremises(premises)
        withPlacementRequest(application.placementRequests[1])
        withApplication(application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.of(2024, 5, 20))
        withCanonicalDepartureDate(LocalDate.of(2024, 8, 1))
        withActualArrivalDate(null)
        withActualArrivalTime(null)
        withActualDepartureDate(null)
        withActualDepartureTime(null)
        withExpectedArrivalDate(LocalDate.of(2024, 5, 20))
        withExpectedDepartureDate(LocalDate.of(2024, 8, 1))
        withCancellationOccurredAt(LocalDate.of(REPORT_YEAR, REPORT_MONTH, 5))
        withCancellationRecordedAt(LocalDate.of(REPORT_YEAR, REPORT_MONTH, 5).atStartOfDay(ZoneId.systemDefault()).toInstant())
        withCancellationReason(cancellationReason)
        withNonArrivalConfirmedAt(null)
        withDepartureReason(null)
        withDepartureNotes(null)
        withMoveOnCategory(null)
        withCriteria(criteria)
        withNonArrivalReason(null)
        withNonArrivalNotes(null)
      }
    }

    fun assertRow(row: PlacementReportRow) {
      assertThat(row.placement_id).isEqualTo(booking.id.toString())
      assertThat(row.expected_arrival_date).isEqualTo("2024-05-20")
      assertThat(row.expected_departure_date).isEqualTo("2024-08-01")
      assertThat(row.actual_arrival_date).isEqualTo(null)
      assertThat(row.actual_arrival_time).isEqualTo(null)
      assertThat(row.actual_departure_date).isEqualTo(null)
      assertThat(row.actual_departure_time).isEqualTo(null)
      assertThat(row.premises_name).isEqualTo("premisesName")
      assertThat(row.premises_region).isEqualTo("southWest")
      assertThat(row.actual_duration_nights).isNull()
      assertThat(row.expected_duration_nights).isEqualTo("73")
      assertThat(row.criteria?.split(", ")).containsExactlyInAnyOrder("isCatered", "isStepFreeDesignated")
      assertThat(row.departure_move_on_category).isNull()
      assertThat(row.departure_reason).isNull()
      assertThat(row.non_arrival_reason).isNull()
      assertThat(row.non_arrival_recorded_date_time).isNull()
      assertThat(row.placement_withdrawn_date).isEqualTo("2024-02-05")
      assertThat(row.placement_withdrawal_recorded_date_time).isEqualTo("2024-02-05T00:00:00Z")
      assertThat(row.placement_withdrawn_reason).isEqualTo("Cancelled")
    }
  }

  inner class BookingsOutsideReportingRange {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createOutOfRangeBookings(outOfRangeArrivalDate: LocalDate, outOfRangeDepartureDate: LocalDate) {
      application = createSubmitAndAssessedApplication(
        crn = "CRNTest2",
        arrivalDateOnApplication = outOfRangeArrivalDate,
      )

      createPlacementApplication(
        application = application,
        placementType = PlacementType.rotl,
        placementDates = listOf(
          PlacementDates(
            expectedArrival = outOfRangeArrivalDate,
            duration = 5,
          ),
        ),
      )

      cas1SpaceBookingEntityFactory.produceAndPersistMultiple(10) {
        withPremises(premises)
        withPlacementRequest(application.placementRequests[0])
        withApplication(application)
        withCreatedBy(user)
        withCanonicalArrivalDate(outOfRangeArrivalDate)
        withCanonicalDepartureDate(outOfRangeDepartureDate)
        withNonArrivalConfirmedAt(outOfRangeArrivalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        withCancellationRecordedAt(outOfRangeDepartureDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
    }
  }

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    placementType: PlacementType,
    placementDates: List<PlacementDates>,
  ) {
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
      ),
    )

    cas1SimpleApiClient.placementApplicationReallocate(
      integrationTestBase = this,
      placementApplicationId = getPlacementApplication(application).id,
      NewReallocation(userId = assessor.id),
    )

    cas1SimpleApiClient.placementApplicationDecision(
      integrationTestBase = this,
      placementApplicationId = getPlacementApplication(application).id,
      assessorJwt = assessorJwt,
      body = PlacementApplicationDecisionEnvelope(
        decision = PlacementApplicationDecision.accepted,
        summaryOfChanges = "summary",
        decisionSummary = "decisionSummary",
      ),
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
      withApplicationSchema(applicationSchema)
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

  private fun acceptLatestAssessment(
    applicationId: UUID,
    expectedArrival: LocalDate?,
    duration: Int,
  ) {
    val assessmentId = getLatestAssessment(applicationId).id

    val essentialCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isESAP)
    val desirableCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.acceptsSexOffenders)

    val placementRequirements = PlacementRequirements(
      gender = Gender.male,
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

  private fun getLatestAssessment(applicationId: UUID) = getApplication(applicationId)
    .assessments.filter { it.reallocatedAt == null }.maxByOrNull { it.createdAt }!!

  private fun getApplication(applicationId: UUID) = realApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

  private fun getPlacementApplications(application: ApplicationEntity) = placementApplicationRepository.findByApplication(application).filter { it.reallocatedAt == null }

  private fun getPlacementApplication(application: ApplicationEntity) = getPlacementApplications(application).first()

  private fun getReportUrl(reportName: Cas1ReportName, year: Int, month: Int) = "/cas1/reports/${reportName.value}?year=$year&month=$month"
}

@SuppressWarnings("ConstructorParameterNaming")
data class PlacementReportRow(
  val placement_id: String?,
  val premises_name: String?,
  val premises_region: String?,
  val expected_arrival_date: String?,
  val expected_duration_nights: String?,
  val expected_departure_date: String?,
  val actual_arrival_date: String?,
  val actual_arrival_time: String?,
  val actual_departure_date: String?,
  val actual_departure_time: String?,
  val actual_duration_nights: String?,
  val departure_reason: String?,
  val departure_move_on_category: String?,
  val non_arrival_recorded_date_time: String?,
  val non_arrival_reason: String?,
  val placement_withdrawn_date: String?,
  val placement_withdrawal_recorded_date_time: String?,
  val placement_withdrawn_reason: String?,
  val criteria: String?,
)
