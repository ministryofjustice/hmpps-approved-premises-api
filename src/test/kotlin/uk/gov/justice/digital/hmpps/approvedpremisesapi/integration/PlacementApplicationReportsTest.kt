package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulRegistrationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserTeamMembership
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PlacementApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZonedDateTime
import java.util.UUID

class PlacementApplicationReportsTest : IntegrationTestBase() {

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var realAssessmentRepository: AssessmentRepository

  @Autowired
  lateinit var realBookingRepository: BookingRepository

  @Autowired
  lateinit var realAppealRepository: AppealRepository

  @Autowired
  lateinit var realOffenderService: OffenderService

  @Autowired
  lateinit var apDeliusContextApiClient: ApDeliusContextApiClient

  lateinit var referrerDetails: Pair<UserEntity, String>
  lateinit var referrerTeam: StaffUserTeamMembership
  lateinit var referrerProbationArea: String

  lateinit var assessorDetails: Pair<UserEntity, String>
  lateinit var managerDetails: Pair<UserEntity, String>
  lateinit var workflowManagerDetails: Pair<UserEntity, String>
  lateinit var matcherDetails: Pair<UserEntity, String>
  lateinit var appealManagerDetails: Pair<UserEntity, String>

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var placementApplicationSchema: ApprovedPremisesPlacementApplicationJsonSchemaEntity

  @BeforeEach
  fun setup() {
    referrerTeam = StaffUserTeamMembershipFactory().produce()
    referrerProbationArea = "Referrer probation area"

    referrerDetails = `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(
          listOf(
            referrerTeam,
          ),
        )
        withProbationAreaDescription(
          referrerProbationArea,
        )
      },
    )
    assessorDetails = `Given a User`(
      roles = listOf(UserRole.CAS1_ASSESSOR),
      staffUserDetailsConfigBlock = {
        withProbationAreaCode("N03")
      },
    )
    managerDetails = `Given a User`(roles = listOf(UserRole.CAS1_MANAGER))
    workflowManagerDetails = `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER))
    matcherDetails = `Given a User`(roles = listOf(UserRole.CAS1_MATCHER))
    appealManagerDetails = `Given a User`(roles = listOf(UserRole.CAS1_APPEALS_MANAGER))

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    placementApplicationSchema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
  }

  @Test
  fun `Get placement application report returns 403 Forbidden if user does not have all regions access`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/reports/placement-applications?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get placement application report returns 400 if month is provided and not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/placement-applications?year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @SuppressWarnings("detekt:LongMethod") // This should be resolvable by using @ParameterizedTest, but this refactoring isn't trivial to do in a way that doesn't just move the warning
  @Test
  fun `Get placement application report returns OK with correct applications`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
      GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

      val singleDateUnsubmittedPlacement = expectedRow {
        application = createAssessedApplication("singleDateUnsubmittedPlacement")
        placementApplicationId = createPlacementApplication(application).id
      }

      val singleDateNoBookingReallocated = expectedRow {
        isAccepted = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateNoBookingReallocated")

        val initialPlacementApplication = createAndSubmitPlacementApplication(application, listOf(placementDate!!))
        reallocatePlacementApplication(initialPlacementApplication[0])

        val reallocatedPlacementApplication = placementApplicationTestRepository.findByApplicationAndReallocatedAtNull(application)
        acceptPlacementApplication(reallocatedPlacementApplication.id)
        placementApplicationId = reallocatedPlacementApplication.id
      }

      val singleDateBooked = expectedRow {
        hasBooking = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateBooked")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
      }

      val singleDateBookedPlacementApplicationSubmittedOutsideOfDateRange = expectedRow {
        hasBooking = true

        placementDate = placementDateStarting(LocalDate.now().plusMonths(2))
        application = createAssessedApplication("singleDateBookedOutOfDateRange")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        placementApplicationTestRepository.updateSubmittedOn(placementApplicationId, OffsetDateTime.now().plusMonths(2))

        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
      }

      val singleDateReallocatedAssessment = expectedRow {
        hasBooking = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAndSubmitApplication("singleDateReallocatedAssessment")
        reallocateAssessment(application)
        acceptAssessmentForApplication(application)
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
      }

      val singleDateWithArrival = expectedRow {
        hasBooking = true
        hasArrival = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateWithArrival")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
        markBookingAsArrived(booking!!)
      }

      val singleDateWithDeparture = expectedRow {
        hasBooking = true
        hasArrival = true
        hasDeparture = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateWithDeparture")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
        markBookingAsDeparted(booking!!)
      }

      val singleDateWithCancellation = expectedRow {
        hasBooking = true
        hasCancellation = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateWithCancellation")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
        cancelBooking(booking!!)
      }

      val singleDateWithNonArrival = expectedRow {
        hasBooking = true
        hasNonArrival = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateWithNonArrival")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        booking = createBookingForPlacementRequest(application.getLatestPlacementRequest()!!)
        markBookingAsNonArrived(booking!!)
        booking = bookingRepository.getReferenceById(booking!!.id)
      }

      val singleDateWithWithdrawal = expectedRow {
        isAccepted = true
        isWithdrawn = true

        placementDate = placementDateStarting(LocalDate.now())
        application = createAssessedApplication("singleDateWithWithdrawal")
        placementApplicationId = createAndAcceptPlacementApplication(application, listOf(placementDate!!))[0].id
        withdrawApplication(application)
      }

      val (multiDateNoneBooked1, multiDateNoneBooked2) = run {
        val placementDate1 = placementDateStarting(LocalDate.now())
        val placementDate2 = placementDateStarting(LocalDate.now().plusMonths(1))
        val application = createAssessedApplication("multiDateNoneBooked")
        val placementApplications = createAndSubmitPlacementApplication(application, listOf(placementDate1, placementDate2))
        val placementApp1 = placementApplications[0]
        val placementApp2 = placementApplications[1]

        listOf(
          expectedRow {
            hasBooking = false
            placementDate = placementDate1
            this.application = application
            placementApplicationId = placementApp1.id
          },
          expectedRow {
            hasBooking = false
            placementDate = placementDate2
            this.application = application
            placementApplicationId = placementApp2.id
          },
        )
      }

      val (multiDateAllBooked1, multiDateAllBooked2) = run {
        val placementDate1 = placementDateStarting(LocalDate.now())
        val placementDate2 = placementDateStarting(LocalDate.now().plusMonths(1))
        val application = createAssessedApplication("multiDateAllBooked")
        val placementApplications = createAndAcceptPlacementApplication(application, listOf(placementDate1, placementDate2))
        val placementApp1 = placementApplications[0]
        val placementApp2 = placementApplications[1]
        val placementRequest1 = placementRequestTestRepository.findByPlacementApplication_id(placementApp1.id)
        val placementRequest2 = placementRequestTestRepository.findByPlacementApplication_id(placementApp2.id)

        listOf(
          expectedRow {
            hasBooking = true
            placementDate = placementDate1
            this.application = application
            placementApplicationId = placementApp1.id
            booking = createBookingForPlacementRequest(placementRequest1!!)
          },
          expectedRow {
            hasBooking = true
            placementDate = placementDate2
            this.application = application
            placementApplicationId = placementApp2.id
            booking = createBookingForPlacementRequest(placementRequest2!!)
          },
        )
      }

      val (multiDateSomeBooked1, multiDateSomeBooked2) = run {
        val placementDate1 = placementDateStarting(LocalDate.now())
        val placementDate2 = placementDateStarting(LocalDate.now().plusMonths(1))
        val application = createAssessedApplication("multiDateSomeBooked")
        val placementApplications = createAndAcceptPlacementApplication(application, listOf(placementDate1, placementDate2))
        val placementApp1 = placementApplications[0]
        val placementApp2 = placementApplications[1]
        val placementRequest2 = placementRequestTestRepository.findByPlacementApplication_id(placementApp2.id)

        listOf(
          expectedRow {
            isAccepted = true
            hasBooking = false
            placementDate = placementDate1
            this.application = application
            placementApplicationId = placementApp1.id
          },
          expectedRow {
            isAccepted = true
            hasBooking = true
            placementDate = placementDate2
            this.application = application
            placementApplicationId = placementApp2.id
            booking = createBookingForPlacementRequest(placementRequest2!!)
          },
        )
      }

      val acceptedAppeal = run {
        val placementDate = placementDateStarting(LocalDate.now())
        val (application, assessment) = createRejectedApplication("acceptedAppeal")
        val (_, newAssessment) = acceptAppealForAssessment(assessment)
        acceptAssessment(newAssessment)
        val placementApplication = createAndAcceptPlacementApplication(application, listOf(placementDate))[0]

        expectedRow {
          this.isAccepted = true
          this.application = application
          this.hasAppeal = true
          this.placementDate = placementDate
          this.placementApplicationId = placementApplication.id
        }
      }

      val rejectedAppeal = run {
        val placementDate = placementDateStarting(LocalDate.now())
        val (application, assessment) = createRejectedApplication("rejectedAppeal")
        rejectAppealForAssessment(assessment)

        expectedRow {
          this.isAccepted = false
          this.application = application
          this.hasAppeal = true
          this.placementDate = placementDate
        }
      }

      val multipleAppeals = run {
        val placementDate = placementDateStarting(LocalDate.now())
        val (application, assessment) = createRejectedApplication("multipleAppeals")
        rejectAppealForAssessment(assessment)
        acceptAppealForAssessment(assessment)
        val (_, newAssessment) = acceptAppealForAssessment(assessment)
        acceptAssessment(newAssessment)
        val placementApplication = createAndAcceptPlacementApplication(application, listOf(placementDate))[0]

        expectedRow {
          this.isAccepted = true
          this.application = application
          this.hasAppeal = true
          this.placementDate = placementDate
          this.placementApplicationId = placementApplication.id
        }
      }

      webTestClient.get()
        .uri("/reports/placement-applications?year=${LocalDate.now().year}&month=${LocalDate.now().monthValue}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actualRows = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<PlacementApplicationReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actualRows.size).isEqualTo(16)

          val unsubmittedPlacementApplication = getPlacementApplication(singleDateUnsubmittedPlacement.application)
          assertThat(actualRows).noneMatch { row -> row.placementRequestId == unsubmittedPlacementApplication.id.toString() }

          val outOfDateRangePlacementApplication = getPlacementApplication(singleDateBookedPlacementApplicationSubmittedOutsideOfDateRange.application)
          assertThat(actualRows).noneMatch { row -> row.placementRequestId == outOfDateRangePlacementApplication.id.toString() }

          assertThat(actualRows).noneMatch { row -> row.crn == rejectedAppeal.application.crn }

          assertApplicationRowHasCorrectData(actualRows, singleDateNoBookingReallocated, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateBooked, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateReallocatedAssessment, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateWithArrival, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateWithDeparture, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateWithCancellation, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateWithNonArrival, userEntity)
          assertApplicationRowHasCorrectData(actualRows, singleDateWithWithdrawal, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multiDateNoneBooked1, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multiDateNoneBooked2, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multiDateAllBooked1, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multiDateAllBooked2, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multiDateSomeBooked1, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multiDateSomeBooked2, userEntity)
          assertApplicationRowHasCorrectData(actualRows, acceptedAppeal, userEntity)
          assertApplicationRowHasCorrectData(actualRows, multipleAppeals, userEntity)
        }
    }
  }

  fun expectedRow(builder: ExpectedRow.() -> Unit): ExpectedRow {
    val row = ExpectedRow()
    builder(row)
    return row
  }

  class ExpectedRow {
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var placementApplicationId: UUID
    var placementDate: PlacementDates? = null
    var booking: BookingEntity? = null

    var isAccepted: Boolean = false
    var hasBooking: Boolean = false
    var hasArrival: Boolean = false
    var hasDeparture: Boolean = false
    var hasCancellation: Boolean = false
    var hasNonArrival: Boolean = false
    var isWithdrawn: Boolean = false
    var hasAppeal: Boolean = false
  }

  private fun ApprovedPremisesApplicationEntity.getAppropriateAssessment(hasAppeal: Boolean): AssessmentEntity? = when (hasAppeal) {
    // By the time the assertions are made, a newer assessment will have automatically been made for accepted appeals.
    // To correctly assert on accepted appeals, the assessment that should be used is the latest one that was *rejected*,
    // not the latest one of any status.
    true -> this.assessments.filter { it.decision == AssessmentDecision.REJECTED }.maxByOrNull { it.createdAt }
    false -> this.getLatestAssessment()
  }

  private fun assertApplicationRowHasCorrectData(
    report: List<PlacementApplicationReportRow>,
    expectedRow: ExpectedRow,
    userEntity: UserEntity,
  ) {
    val applicationId = expectedRow.application.id
    val booking = expectedRow.booking
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val placementApplication = placementApplicationTestRepository.findById(expectedRow.placementApplicationId).get()
    val assessment = application.getAppropriateAssessment(expectedRow.hasAppeal)!!
    val offenderDetailSummary = getOffenderDetailForApplication(application, userEntity.deliusUsername)

    val reportRow = report.find {
      it.placementRequestId == placementApplication.id.toString() &&
        it.requestedArrivalDate == expectedRow.placementDate!!.expectedArrival
    }!!

    val (referrerEntity, _) = referrerDetails

    assertThat(reportRow.crn).isEqualTo(application.crn)

    assertThat(reportRow.placementRequestSubmittedAt).isEqualTo(placementApplication.submittedAt!!.toLocalDate())
    assertThat(reportRow.requestedArrivalDate).isEqualTo(expectedRow.placementDate!!.expectedArrival)
    assertThat(reportRow.requestedDurationDays).isEqualTo(expectedRow.placementDate!!.duration)

    if (expectedRow.isAccepted || expectedRow.hasBooking) {
      assertThat(reportRow.decision).isEqualTo(if (expectedRow.isWithdrawn) "WITHDRAW" else "ACCEPTED")
      assertThat(reportRow.decisionMadeAt).isToday()
    } else {
      assertThat(reportRow.decision).isNull()
      assertThat(reportRow.decisionMadeAt).isNull()
    }

    assertThat(reportRow.applicationSubmittedAt).isEqualTo(application.submittedAt!!.toLocalDate())
    assertThat(reportRow.applicationAssessedDate).isEqualTo(assessment.submittedAt!!.toLocalDate())
    assertThat(reportRow.assessorCru).isEqualTo("Wales")

    if (!expectedRow.hasAppeal) {
      assertThat(reportRow.assessmentDecision).isEqualTo(assessment.decision.toString())
    }
    assertThat(reportRow.assessmentDecisionRationale).isEqualTo(assessment.rejectionRationale)
    assertThat(reportRow.ageInYears).isEqualTo(Period.between(offenderDetailSummary.dateOfBirth, LocalDate.now()).years)
    assertThat(reportRow.gender).isEqualTo(offenderDetailSummary.gender)
    assertThat(reportRow.mappa).isEqualTo(application.riskRatings!!.mappa.value!!.level)
    assertThat(reportRow.offenceId).isEqualTo(application.offenceId)
    assertThat(reportRow.noms).isEqualTo(application.nomsNumber)
    assertThat(reportRow.sentenceType).isEqualTo(application.sentenceType)
    assertThat(reportRow.releaseType).isEqualTo(application.releaseType)

    val caseDetail = getCaseDetailForApplication(application)
    assertThat(reportRow.referralLdu).isEqualTo(caseDetail.case.manager.team.ldu.name)
    assertThat(reportRow.referralTeam).isEqualTo(caseDetail.case.manager.team.name)

    assertThat(reportRow.referralRegion).isEqualTo(referrerProbationArea)
    assertThat(reportRow.referrerUsername).isEqualTo(referrerEntity.deliusUsername)
    assertThat(reportRow.targetLocation).isEqualTo(application.targetLocation)
    assertThat(reportRow.applicationWithdrawalReason).isEqualTo(application.withdrawalReason)
    assertThat(reportRow.applicationWithdrawalReason).isEqualTo(application.withdrawalReason)

    if (expectedRow.isWithdrawn) {
      assertThat(reportRow.applicationWithdrawalReason).isEqualTo(application.withdrawalReason)
      assertThat(reportRow.applicationWithdrawalDate).isEqualTo(LocalDate.now())
    }

    if (expectedRow.hasBooking) {
      checkNotNull(booking)

      assertThat(reportRow.bookingID).isEqualTo(booking.id.toString())
      assertThat(reportRow.bookingMadeDate).isToday()

      if (expectedRow.hasCancellation) {
        val cancellation = booking.cancellation!!
        assertThat(reportRow.bookingCancellationDate).isEqualTo(cancellation.date)
      }

      val placementDate = checkNotNull(expectedRow.placementDate)
      assertThat(reportRow.expectedArrivalDate).isEqualTo(placementDate.expectedArrival)
      assertThat(reportRow.expectedDepartureDate).isEqualTo(placementDate.expectedArrival.plusDays(placementDate.duration.toLong()))
      assertThat(reportRow.premisesName).isEqualTo(booking.premises.name)
    }

    if (expectedRow.hasArrival) {
      checkNotNull(booking)
      val arrival = booking.arrival!!
      assertThat(reportRow.actualArrivalDate).isEqualTo(arrival.arrivalDate)
    }

    if (expectedRow.hasDeparture) {
      checkNotNull(booking)
      val departure = booking.departure!!
      assertThat(reportRow.actualDepartureDate).isEqualTo(departure.dateTime.toLocalDate())
      assertThat(reportRow.departureReason).isEqualTo(departure.reason.name)
      assertThat(reportRow.departureMoveOnCategory).isEqualTo(departure.moveOnCategory.name)
    }

    if (expectedRow.hasNonArrival) {
      val nonArrival = booking!!.nonArrival!!
      assertThat(reportRow.hasNotArrived).isEqualTo(true)
      assertThat(reportRow.notArrivedReason).isEqualTo(nonArrival.reason.name)
    }

    assertThat(reportRow.placementRequestType).isEqualTo("Some Test Reason")
    assertThat(reportRow.paroleDecisionDate).isEqualTo("2023-11-11")

    if (expectedRow.hasAppeal) {
      val appeals = realAppealRepository.findAllByAssessmentId(assessment.id)
      val latestAppeal = appeals.maxByOrNull { it.createdAt }!!
      assertThat(reportRow.assessmentAppealCount).isEqualTo(appeals.size)
      assertThat(reportRow.lastAssessmentAppealedDecision).isEqualTo(latestAppeal.decision)
      assertThat(reportRow.lastAssessmentAppealedDate).isEqualTo(latestAppeal.appealDate)
      assertThat(reportRow.assessmentAppealedFromStatus).isEqualTo(assessment.decision.toString())
      assertThat(reportRow.assessmentDecision).isEqualTo(latestAppeal.decision)
    }
  }

  fun getPlacementApplication(application: ApprovedPremisesApplicationEntity) = placementApplicationTestRepository.findByApplicationAndReallocatedAtNull(application)

  private fun getOffenderDetailForApplication(
    application: ApplicationEntity,
    deliusUsername: String,
  ): OffenderDetailSummary {
    return when (val personInfo = realOffenderService.getInfoForPerson(application.crn, deliusUsername, true)) {
      is PersonInfoResult.Success.Full -> personInfo.offenderDetailSummary
      else -> error("No offender found for CRN ${application.crn}")
    }
  }

  private fun getCaseDetailForApplication(application: ApplicationEntity): CaseDetail {
    return when (val caseDetailResult = apDeliusContextApiClient.getCaseDetail(application.crn)) {
      is ClientResult.Success -> caseDetailResult.body
      is ClientResult.Failure -> caseDetailResult.throwException()
    }
  }

  private fun createAssessedApplication(crn: String): ApprovedPremisesApplicationEntity {
    val application = createAndSubmitApplication(crn)
    acceptAssessmentForApplication(application)
    return application
  }

  private fun createRejectedApplication(crn: String): Pair<ApprovedPremisesApplicationEntity, ApprovedPremisesAssessmentEntity> {
    val application = createAndSubmitApplication(crn)
    val assessment = rejectAssessmentForApplication(application)
    return application to assessment
  }

  private fun createAndSubmitApplication(crn: String): ApprovedPremisesApplicationEntity {
    val (referrer, jwt) = referrerDetails
    val (offenderDetails, _) = `Given an Offender`({ withCrn(crn) })

    CommunityAPI_mockSuccessfulRegistrationsCall(
      offenderDetails.otherIds.crn,
      Registrations(
        registrations = listOf(
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "MAPP", description = "MAPPA"))
            .withRegisterCategory(RegistrationKeyValue(code = "M2", description = "M2"))
            .withRegisterLevel(RegistrationKeyValue(code = "M2", description = "M2"))
            .withStartDate(LocalDate.parse("2022-09-06"))
            .produce(),
        ),
      ),
    )

    APDeliusContext_mockSuccessfulCaseDetailCall(
      offenderDetails.otherIds.crn,
      CaseDetailFactory()
        .from(offenderDetails.asCaseDetail())
        .withMappaDetail(
          MappaDetail(
            2,
            "M2",
            2,
            "M2",
            LocalDate.parse("2022-09-06"),
            ZonedDateTime.parse("2022-09-06T00:00:00Z"),
          ),
        )
        .produce(),
    )

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(referrer)
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
      withApplicationSchema(applicationSchema)
      withData(
        objectMapper.writeValueAsString(
          mapOf(
            "basic-information" to
              mapOf(
                "sentence-type"
                  to mapOf(
                    "sentenceType" to SentenceTypeOption.nonStatutory.value,
                  ),
              ),
          ),
        ),
      )
      withRiskRatings(
        PersonRisksFactory()
          .withMappa(
            RiskWithStatus(
              status = RiskStatus.Retrieved,
              value = Mappa(
                level = "CAT M2/LEVEL M2",
                lastUpdated = LocalDate.now(),
              ),
            ),
          ).produce(),
      )
    }

    val apType = ApType.normal
    webTestClient.post()
      .uri("/applications/${application.id}/submission")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        SubmitApprovedPremisesApplication(
          translatedDocument = {},
          isPipeApplication = apType == ApType.pipe,
          isWomensApplication = false,
          isEmergencyApplication = false,
          isEsapApplication = apType == ApType.esap,
          targetLocation = "SW1A 1AA",
          releaseType = ReleaseTypeOption.licence,
          type = "CAS1",
          sentenceType = SentenceTypeOption.nonStatutory,
          applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
          caseManagerIsNotApplicant = false,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    return realApplicationRepository.findByIdOrNull(application.id) as ApprovedPremisesApplicationEntity
  }

  private fun acceptAssessmentForApplication(application: ApprovedPremisesApplicationEntity): ApprovedPremisesAssessmentEntity {
    val (assessorEntity, jwt) = assessorDetails

    val assessment = realAssessmentRepository.findByApplication_IdAndReallocatedAtNull(application.id)!!

    return acceptAssessment(assessment)
  }

  private fun acceptAssessment(assessment: AssessmentEntity): ApprovedPremisesAssessmentEntity {
    val (assessorEntity, jwt) = assessorDetails

    val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

    assessment.data = "{}"
    assessment.allocatedToUser = assessorEntity
    realAssessmentRepository.save(assessment)

    val essentialCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isESAP)
    val desirableCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.acceptsSexOffenders)

    val placementRequirements = PlacementRequirements(
      gender = Gender.male,
      type = ApType.normal,
      location = postcodeDistrict.outcode,
      radius = 50,
      essentialCriteria = essentialCriteria,
      desirableCriteria = desirableCriteria,
    )

    webTestClient.post()
      .uri("/assessments/${assessment.id}/acceptance")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequirements))
      .exchange()
      .expectStatus()
      .isOk

    return realAssessmentRepository.findByIdOrNull(assessment.id) as ApprovedPremisesAssessmentEntity
  }

  private fun rejectAssessmentForApplication(application: ApprovedPremisesApplicationEntity): ApprovedPremisesAssessmentEntity {
    val (assessorEntity, jwt) = assessorDetails

    val assessment = realAssessmentRepository.findByApplication_IdAndReallocatedAtNull(application.id)!!

    assessment.data = "{}"
    assessment.allocatedToUser = assessorEntity
    realAssessmentRepository.save(assessment)

    webTestClient.post()
      .uri("/assessments/${assessment.id}/rejection")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(AssessmentRejection(document = mapOf("document" to "value"), rejectionRationale = "Some reason"))
      .exchange()
      .expectStatus()
      .isOk

    return realAssessmentRepository.findByIdOrNull(assessment.id) as ApprovedPremisesAssessmentEntity
  }

  private fun createBookingForPlacementRequest(placementRequest: PlacementRequestEntity): BookingEntity {
    val (_, jwt) = matcherDetails

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val room = roomEntityFactory.produceAndPersist { withPremises(premises) }
    val bed = bedEntityFactory.produceAndPersist { withRoom(room) }

    webTestClient.post()
      .uri("/placement-requests/${placementRequest.id}/booking")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPlacementRequestBooking(
          arrivalDate = placementRequest.expectedArrival,
          departureDate = placementRequest.expectedArrival.plusDays(placementRequest.duration.toLong()),
          bedId = bed.id,
          premisesId = premises.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    return placementRequestRepository.getReferenceById(placementRequest.id).booking!!
  }

  private fun markBookingAsArrived(booking: BookingEntity): ArrivalEntity {
    val (_, jwt) = managerDetails
    val premises = booking.premises as ApprovedPremisesEntity
    val keyWorker = ContextStaffMemberFactory().produce()
    val staffUserDetails = StaffUserDetailsFactory().produce()
    APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)
    APDeliusContext_mockSuccessfulStaffDetailsCall(keyWorker.code, staffUserDetails)

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewCas1Arrival(
          type = "CAS1",
          arrivalDateTime = Instant.parse("2022-08-12T15:30:00Z"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = "Hello",
          keyWorkerStaffCode = keyWorker.code,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val persistedBookingEntity = realBookingRepository.findById(booking.id).get()

    return persistedBookingEntity.arrival!!
  }

  private fun markBookingAsDeparted(booking: BookingEntity): DepartureEntity {
    markBookingAsArrived(booking)

    val (_, jwt) = managerDetails

    val reason = departureReasonEntityFactory.produceAndPersist {
      withServiceScope("approved-premises")
    }
    val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
      withServiceScope("approved-premises")
    }
    val destinationProvider = destinationProviderEntityFactory.produceAndPersist()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewDeparture(
          dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
          reasonId = reason.id,
          moveOnCategoryId = moveOnCategory.id,
          destinationProviderId = destinationProvider.id,
          notes = "Hello",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val persistedBookingEntity = realBookingRepository.findById(booking.id).get()

    return persistedBookingEntity.departure!!
  }

  private fun cancelBooking(booking: BookingEntity): CancellationEntity {
    val (_, jwt) = workflowManagerDetails

    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
      withServiceScope("*")
    }

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewCancellation(
          date = LocalDate.parse("2022-08-17"),
          reason = cancellationReason.id,
          notes = null,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val persistedBookingEntity = realBookingRepository.findById(booking.id).get()

    return persistedBookingEntity.cancellation!!
  }

  private fun markBookingAsNonArrived(booking: BookingEntity): NonArrivalEntity {
    val (_, jwt) = managerDetails

    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/non-arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewNonarrival(
          date = booking.arrivalDate,
          reason = nonArrivalReason.id,
          notes = "Notes",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val persistedBookingEntity = realBookingRepository.findById(booking.id).get()

    return persistedBookingEntity.nonArrival!!
  }

  fun placementDateStarting(start: LocalDate): PlacementDates =
    PlacementDates(
      expectedArrival = start,
      duration = 12,
    )

  private fun createAndAcceptPlacementApplication(application: ApprovedPremisesApplicationEntity, placementDates: List<PlacementDates>): List<PlacementApplication> {
    val placementApplications = createAndSubmitPlacementApplication(application, placementDates)
    placementApplications.forEach { acceptPlacementApplication(it.id) }
    return placementApplications
  }

  private fun createPlacementApplication(application: ApprovedPremisesApplicationEntity): PlacementApplication {
    val (_, jwt) = assessorDetails

    val rawResult = webTestClient.post()
      .uri("/placement-applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPlacementApplication(
          applicationId = application.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    return objectMapper.readValue(rawResult, PlacementApplication::class.java)
  }

  private fun createAndSubmitPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    placementDates: List<PlacementDates>,
  ): List<PlacementApplication> {
    val (_, jwt) = assessorDetails

    val placementApplication = createPlacementApplication(application)

    webTestClient.put()
      .uri("/placement-applications/${placementApplication.id}")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdatePlacementApplication(
          data = mapOf(
            "request-a-placement" to mapOf(
              "decision-to-release" to mapOf("decisionToReleaseDate" to "2023-11-11"),
              "reason-for-placement" to mapOf("reason" to "Some Test Reason"),
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val rawResult = webTestClient.post()
      .uri("/placement-applications/${placementApplication.id}/submission")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        SubmitPlacementApplication(
          translatedDocument = mapOf("thingId" to 123),
          placementType = PlacementType.additionalPlacement,
          placementDates = placementDates,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>().responseBody.blockFirst()

    return objectMapper.readerForListOf(PlacementApplication::class.java).readValue(rawResult)
  }

  private fun acceptPlacementApplication(placementApplicationId: UUID) {
    val (_, matcherJwt) = matcherDetails

    webTestClient.post()
      .uri("/placement-applications/$placementApplicationId/decision")
      .header("Authorization", "Bearer $matcherJwt")
      .bodyValue(
        PlacementApplicationDecisionEnvelope(
          decision = PlacementApplicationDecision.accepted,
          summaryOfChanges = "ChangeSummary",
          decisionSummary = "DecisionSummary",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun reallocateAssessment(application: ApprovedPremisesApplicationEntity) {
    val (_, jwt) = workflowManagerDetails
    val (assigneeUser, _) = `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR))

    val existingAssessment = application.getLatestAssessment()!!

    webTestClient.post()
      .uri("/tasks/assessment/${existingAssessment.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(
        NewReallocation(
          userId = assigneeUser.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated

    // When assessments are reallocated, the millisecond part of the new assessment's created_on date is set to 0
    // This can lead to the wrong assessment being selected when creating a placement application
    // The following call ensures the new assessment always has the latest created_on date
    assessmentTestRepository.updateCreatedAtOnLatestAssessment(OffsetDateTime.now(), application.id)
  }

  private fun reallocatePlacementApplication(placementApplication: PlacementApplication) {
    val (_, jwt) = workflowManagerDetails
    val (matcherUser, _) = matcherDetails

    webTestClient.post()
      .uri("/tasks/placement-application/${placementApplication.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(
        NewReallocation(
          userId = matcherUser.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
  }

  private fun withdrawApplication(application: ApprovedPremisesApplicationEntity) {
    val (_, jwt) = referrerDetails

    webTestClient.post()
      .uri("/applications/${application.id}/withdrawal")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(
        NewWithdrawal(
          reason = WithdrawalReason.duplicateApplication,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun acceptAppealForAssessment(assessment: ApprovedPremisesAssessmentEntity): Pair<AppealEntity, AssessmentEntity> {
    val (_, jwt) = appealManagerDetails

    webTestClient.post()
      .uri("/applications/${assessment.application.id}/appeals/")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewAppeal(
          appealDate = LocalDate.now(),
          appealDetail = "Some details about the appeal",
          decision = AppealDecision.accepted,
          decisionDetail = "Some details about why the appeal was accepted",
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated

    val appeal = realAppealRepository.findAllByAssessmentId(assessment.id).maxByOrNull { it.createdAt }!!
    val newAssessment = realApplicationRepository.findByIdOrNull(assessment.application.id)!!.getLatestAssessment()!!

    return appeal to newAssessment
  }

  private fun rejectAppealForAssessment(assessment: ApprovedPremisesAssessmentEntity): AppealEntity {
    val (_, jwt) = appealManagerDetails

    webTestClient.post()
      .uri("/applications/${assessment.application.id}/appeals/")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewAppeal(
          appealDate = LocalDate.now(),
          appealDetail = "Some details about the appeal",
          decision = AppealDecision.rejected,
          decisionDetail = "Some details about why the appeal was rejected",
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated

    return realAppealRepository.findAllByAssessmentId(assessment.id).maxByOrNull { it.createdAt }!!
  }
}
