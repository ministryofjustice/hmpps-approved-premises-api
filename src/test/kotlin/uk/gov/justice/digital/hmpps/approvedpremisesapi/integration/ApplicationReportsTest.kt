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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulRegistrationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

class ApplicationReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var realAssessmentRepository: AssessmentRepository

  @Autowired
  lateinit var realBookingRepository: BookingRepository

  @Autowired
  lateinit var realOffenderService: OffenderService

  @Autowired
  lateinit var apDeliusContextApiClient: ApDeliusContextApiClient

  lateinit var referrerDetails: Pair<UserEntity, String>
  lateinit var referrerTeam: StaffUserTeamMembership
  lateinit var referrerProbationArea: String

  lateinit var assessorDetails: Pair<UserEntity, String>
  lateinit var managerDetails: Pair<UserEntity, String>
  lateinit var matcherDetails: Pair<UserEntity, String>

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity

  @BeforeEach
  fun setup() {
    referrerTeam = StaffUserTeamMembershipFactory().produce()
    referrerProbationArea = "Referrer probation area"

    referrerDetails = `Given a User`(staffUserDetailsConfigBlock = {
      withTeams(
        listOf(
          referrerTeam,
        ),
      )
      withProbationAreaDescription(
        referrerProbationArea,
      )
    },)
    assessorDetails = `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR), staffUserDetailsConfigBlock = {
      withProbationAreaCode("N03")
    },)
    managerDetails = `Given a User`(roles = listOf(UserRole.CAS1_MANAGER))
    matcherDetails = `Given a User`(roles = listOf(UserRole.CAS1_MATCHER))

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
  }

  @Test
  fun `Get application report returns 403 Forbidden if user does not have all regions access`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/reports/applications?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get application report returns 400 if month is provided and not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/applications?year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get application report returns OK with correct applications`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
      val (applicationWithBooking, arrivedBooking) = createApplicationWithBooking()
      val (applicationWithDepartedBooking, departedBooking) = createApplicationWithBooking()
      val (applicationWithCancelledBooking, cancelledBooking) = createApplicationWithBooking()
      val (applicationWithNonArrivedBooking, nonArrivedBooking) = createApplicationWithBooking()

      markBookingAsArrived(arrivedBooking)
      markBookingAsArrived(departedBooking)

      markBookingAsDeparted(departedBooking)

      cancelBooking(cancelledBooking)

      markBookingAsNonArrived(nonArrivedBooking)

      webTestClient.get()
        .uri("/reports/applications?year=${LocalDate.now().year}&month=${LocalDate.now().monthValue}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<ApplicationReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(4)

          val applicationRowWithBooking = actual.find { reportRow -> reportRow.id == applicationWithBooking.id.toString() }!!
          val applicationRowWithDepartedBooking = actual.find { reportRow -> reportRow.id == applicationWithDepartedBooking.id.toString() }!!
          val applicationRowWithCancelledBooking = actual.find { reportRow -> reportRow.id == applicationWithCancelledBooking.id.toString() }!!
          val applicationRowWithNonArrivedBooking = actual.find { reportRow -> reportRow.id == applicationWithNonArrivedBooking.id.toString() }!!

          assertApplicationRowHasCorrectData(applicationWithBooking.id, applicationRowWithBooking, userEntity, hasBooking = true)
          assertApplicationRowHasCorrectData(applicationWithDepartedBooking.id, applicationRowWithDepartedBooking, userEntity, hasBooking = true)
          assertApplicationRowHasCorrectData(applicationWithCancelledBooking.id, applicationRowWithCancelledBooking, userEntity, hasBooking = true)
          assertApplicationRowHasCorrectData(applicationWithNonArrivedBooking.id, applicationRowWithNonArrivedBooking, userEntity, hasBooking = true)
        }
    }
  }

  private fun assertApplicationRowHasCorrectData(applicationId: UUID, reportRow: ApplicationReportRow, userEntity: UserEntity, hasBooking: Boolean = true, hasCancellation: Boolean = false, hasDeparture: Boolean = false, hasNonArrival: Boolean = false) {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!
    val placementRequest = application.getLatestPlacementRequest()!!
    val offenderDetailSummary = getOffenderDetailForApplication(application, userEntity.deliusUsername)
    val caseDetail = getCaseDetailForApplication(application)

    val (referrerEntity, _) = referrerDetails

    assertThat(reportRow.crn).isEqualTo(application.crn)
    assertThat(reportRow.applicationAssessedDate).isEqualTo(assessment.submittedAt!!.toLocalDate())
    assertThat(reportRow.assessorCru).isEqualTo("Wales")
    assertThat(reportRow.assessmentDecision).isEqualTo(assessment.decision.toString())
    assertThat(reportRow.assessmentDecisionRationale).isEqualTo(assessment.rejectionRationale)
    assertThat(reportRow.ageInYears).isEqualTo(Period.between(offenderDetailSummary.dateOfBirth, LocalDate.now()).years)
    assertThat(reportRow.gender).isEqualTo(offenderDetailSummary.gender)
    assertThat(reportRow.mappa).isEqualTo(application.riskRatings!!.mappa.value!!.level)
    assertThat(reportRow.offenceId).isEqualTo(application.offenceId)
    assertThat(reportRow.noms).isEqualTo(application.nomsNumber)
    assertThat(reportRow.premisesType).isEqualTo(placementRequest.placementRequirements.apType.name)
    assertThat(reportRow.releaseType).isEqualTo(application.releaseType)
    assertThat(reportRow.applicationSubmissionDate).isEqualTo(application.submittedAt!!.toLocalDate())
    assertThat(reportRow.targetLocation).isEqualTo(application.targetLocation)
    assertThat(reportRow.applicationWithdrawalReason).isEqualTo(application.withdrawalReason)

    assertThat(reportRow.referrerUsername).isEqualTo(referrerEntity.deliusUsername)
    assertThat(reportRow.referralLdu).isEqualTo(caseDetail.case.manager.team.ldu.name)
    assertThat(reportRow.referralRegion).isEqualTo(referrerProbationArea)
    assertThat(reportRow.referralTeam).isEqualTo(caseDetail.case.manager.team.name)

    if (hasBooking) {
      val booking = placementRequest.booking!!
      assertThat(reportRow.bookingID).isEqualTo(booking.id.toString())
      assertThat(reportRow.expectedArrivalDate).isEqualTo(booking.arrivalDate)
      assertThat(reportRow.expectedDepartureDate).isEqualTo("2022-08-30")
      assertThat(reportRow.premisesName).isEqualTo(booking.premises.name)
    }

    if (hasCancellation) {
      val cancellation = placementRequest.booking!!.cancellation!!
      assertThat(reportRow.bookingCancellationDate).isEqualTo(cancellation.date)
    }

    if (hasDeparture) {
      val arrival = placementRequest.booking!!.arrival!!
      val departure = placementRequest.booking!!.departure!!
      assertThat(reportRow.actualArrivalDate).isEqualTo(arrival.arrivalDate)
      assertThat(reportRow.actualDepartureDate).isEqualTo(departure.dateTime.toLocalDate())
      assertThat(reportRow.departureMoveOnCategory).isEqualTo(departure.moveOnCategory.name)
    }

    if (hasNonArrival) {
      val nonArrival = placementRequest.booking!!.nonArrival!!
      assertThat(reportRow.hasNotArrived).isEqualTo(true)
      assertThat(reportRow.notArrivedReason).isEqualTo(nonArrival.reason.name)
    }
  }

  private fun getOffenderDetailForApplication(application: ApplicationEntity, deliusUsername: String): OffenderDetailSummary {
    return when (val personInfo = realOffenderService.getInfoForPerson(application.crn, deliusUsername, true)) {
      is PersonInfoResult.Success.Full -> personInfo.offenderDetailSummary
      else -> throw Exception("No offender found for CRN ${application.crn}")
    }
  }

  private fun getCaseDetailForApplication(application: ApplicationEntity): CaseDetail {
    return when (val caseDetailResult = apDeliusContextApiClient.getCaseDetail(application.crn)) {
      is ClientResult.Success -> caseDetailResult.body
      is ClientResult.Failure -> caseDetailResult.throwException()
    }
  }

  private fun createApplicationWithBooking(): Pair<ApprovedPremisesApplicationEntity, BookingEntity> {
    val application = createAndSubmitApplication(ApType.normal)
    acceptAssessmentForApplication(application)
    val booking = createBookingForApplication(application)

    return Pair(application, booking)
  }

  private fun createAndSubmitApplication(apType: ApType): ApprovedPremisesApplicationEntity {
    val (referrer, jwt) = referrerDetails
    val (offenderDetails, _) = `Given an Offender`()

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
                    "sentenceType" to "Some Sentence Type",
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
    val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

    assessment.data = "{}"
    assessment.allocatedToUser = assessorEntity
    realAssessmentRepository.save(assessment)

    val essentialCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isESAP)
    val desirableCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.acceptsSexOffenders)

    val placementDates = PlacementDates(
      expectedArrival = LocalDate.now(),
      duration = 12,
    )

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
      .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequirements, placementDates = placementDates))
      .exchange()
      .expectStatus()
      .isOk

    return realAssessmentRepository.findByIdOrNull(assessment.id) as ApprovedPremisesAssessmentEntity
  }

  private fun createBookingForApplication(application: ApprovedPremisesApplicationEntity): BookingEntity {
    val (_, jwt) = matcherDetails

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewBooking(
          crn = application.crn,
          arrivalDate = LocalDate.parse("2022-08-12"),
          departureDate = LocalDate.parse("2022-08-30"),
          serviceName = ServiceName.approvedPremises,
          bedId = bed.id,
          eventNumber = "eventNumber",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    return realBookingRepository.findByApplication(application)
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
    val (_, jwt) = managerDetails

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
}
