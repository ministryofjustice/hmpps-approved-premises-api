package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromNestedAuthorisableValidatableActionResult
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
@Service
class Cas1ApplicationSeedService(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val bookingRepository: BookingRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val offenderService: OffenderService,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val environmentService: EnvironmentService,
  private val assessmentService: AssessmentService,
  private val assessmentRepository: AssessmentRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  enum class ApplicationState {
    PENDING_SUBMISSION,
    AUTHORISED,
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  fun createApplication(
    deliusUserName: String,
    crn: String,
    createIfExistingApplicationForCrn: Boolean = false,
    state: ApplicationState,
  ) {
    if (environmentService.isNotATestEnvironment()) {
      error("Cannot create test applications as not in a test environment")
    }

    createApplicationInternal(deliusUserName, crn, createIfExistingApplicationForCrn, state)
  }

  fun createOfflineApplicationWithBooking(deliusUserName: String, crn: String) {
    if (environmentService.isNotATestEnvironment()) {
      error("Cannot create test applications as not in a test environment")
    }

    createOfflineApplicationInternal(deliusUserName, crn)
  }

  private fun createApplicationInternal(
    deliusUserName: String,
    crn: String,
    createIfExistingApplicationForCrn: Boolean,
    state: ApplicationState,
  ) {
    if (!createIfExistingApplicationForCrn && applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises).isNotEmpty()) {
      log.info("Already have CAS1 application for $crn, not seeding a new application")
      return
    }

    log.info("Auto creating a CAS1 application for $crn with state $state")

    when (state) {
      ApplicationState.PENDING_SUBMISSION -> {
        createApplicationPendingSubmission(deliusUserName, crn)
      }
      ApplicationState.AUTHORISED -> {
        val application = createApplicationPendingSubmission(deliusUserName, crn)
        submitApplication(application)
        assessAndAcceptApplication(application)
      }
    }
  }

  private fun createApplicationPendingSubmission(
    deliusUserName: String,
    crn: String,
  ): ApprovedPremisesApplicationEntity {
    val personInfo = getPersonInfo(crn)
    val createdByUser = (userService.getExistingUserOrCreate(deliusUserName) as GetUserResponse.Success).user

    val newApplicationEntity = extractEntityFromCasResult(
      applicationService.createApprovedPremisesApplication(
        offenderDetails = personInfo.offenderDetailSummary,
        user = createdByUser,
        convictionId = 2500295345,
        deliusEventNumber = "2",
        offenceId = "M2500295343",
        createWithRisks = true,
      ),
    )

    val updatedApplication = extractEntityFromCasResult(
      applicationService.updateApprovedPremisesApplication(
        applicationId = newApplicationEntity.id,
        updateFields = ApplicationService.Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = null,
          apType = ApType.normal,
          releaseType = "licence",
          arrivalDate = LocalDate.of(2025, 12, 12),
          data = loadFixtureAsResource("application_data.json"),
          isInapplicable = false,
          noticeType = Cas1ApplicationTimelinessCategory.standard,
        ),
        userForRequest = createdByUser,
      ),
    )

    applicationTimelineNoteService.saveApplicationTimelineNote(
      applicationId = newApplicationEntity.id,
      note = "Application automatically created by Cas1 Seeding",
      user = null,
    )

    return updatedApplication
  }

  private fun submitApplication(
    application: ApprovedPremisesApplicationEntity,
  ) {
    val user = application.createdByUser

    applicationService.submitApprovedPremisesApplication(
      applicationId = application.id,
      submitApplication = SubmitApprovedPremisesApplication(
        apType = ApType.normal,
        translatedDocument = JSONObject(loadFixtureAsResource("application_document.json")).toMap(),
        caseManagerIsNotApplicant = false,
        isWomensApplication = false,
        releaseType = ReleaseTypeOption.licence,
        targetLocation = postcodeDistrictRepository.findAll()[0].outcode,
        arrivalDate = LocalDate.of(2030, 1, 1),
        sentenceType = SentenceTypeOption.ipp,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails(
          name = user.name,
          email = user.email,
          telephoneNumber = user.telephoneNumber,
        ),
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        licenseExpiryDate = LocalDate.of(2050, 1, 1),
        type = "approved-premises",
      ),
      user = user,
      apAreaId = user.apArea!!.id,
    )
  }

  private fun assessAndAcceptApplication(application: ApprovedPremisesApplicationEntity) {
    val assessor = application.createdByUser

    extractEntityFromNestedAuthorisableValidatableActionResult(
      assessmentService.reallocateAssessment(
        id = getAssessmentId(application),
        allocatingUser = assessor,
        assigneeUser = assessor,
      ),
    )

    ensureEntityFromCasResultIsSuccess(
      assessmentService.updateAssessment(
        assessmentId = getAssessmentId(application),
        updatingUser = assessor,
        data = loadFixtureAsResource("assessment_data.json"),
      ),
    )

    ensureEntityFromCasResultIsSuccess(
      assessmentService.acceptAssessment(
        acceptingUser = assessor,
        assessmentId = getAssessmentId(application),
        document = loadFixtureAsResource("assessment_document.json"),
        placementRequirements = PlacementRequirements(
          gender = Gender.male,
          type = ApType.normal,
          location = application.targetLocation!!,
          radius = 25,
          essentialCriteria = emptyList(),
          desirableCriteria = emptyList(),
        ),
        placementDates = PlacementDates(
          expectedArrival = application.arrivalDate!!.toLocalDate(),
          duration = 28,
        ),
        apType = ApType.normal,
        notes = null,
      ),
    )
  }

  private fun getAssessmentId(application: ApprovedPremisesApplicationEntity) = assessmentRepository.findByApplicationIdAndReallocatedAtNull(applicationId = application.id)!!.id

  private fun createOfflineApplicationInternal(deliusUserName: String, crn: String) {
    if (applicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises).isNotEmpty()) {
      log.info("Already have an offline CAS1 application for $crn, not seeding a new application")
      return
    }

    val personInfo = getPersonInfo(crn)
    val offenderDetail = personInfo.offenderDetailSummary

    val offlineApplication = applicationService.createOfflineApplication(
      OfflineApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        service = ServiceName.approvedPremises.value,
        createdAt = OffsetDateTime.now(),
        eventNumber = "2",
        name = "${offenderDetail.firstName.uppercase()} ${offenderDetail.surname.uppercase()}",
      ),
    )

    val bookingArrivalDate = LocalDate.of(2027, 1, 2)
    val bookingDepartureDate = LocalDate.of(2027, 1, 6)

    bookingRepository.save(
      BookingEntity(
        id = UUID.randomUUID(),
        crn = crn,
        arrivalDate = bookingArrivalDate,
        departureDate = bookingDepartureDate,
        keyWorkerStaffCode = null,
        arrivals = mutableListOf(),
        departures = mutableListOf(),
        nonArrival = null,
        cancellations = mutableListOf(),
        confirmation = null,
        extensions = mutableListOf(),
        premises = approvedPremisesRepository.findAll().first(),
        bed = null,
        service = ServiceName.approvedPremises.value,
        originalArrivalDate = bookingArrivalDate,
        originalDepartureDate = bookingDepartureDate,
        createdAt = OffsetDateTime.now(),
        application = null,
        offlineApplication = offlineApplication,
        turnarounds = mutableListOf(),
        dateChanges = mutableListOf(),
        nomsNumber = personInfo.offenderDetailSummary.otherIds.nomsNumber,
        placementRequest = null,
        status = BookingStatus.confirmed,
        adhoc = true,
      ),
    )

    val spaceBookingArrivalDate = LocalDate.of(2028, 5, 12)
    val spaceBookingDepartureDate = LocalDate.of(2028, 7, 6)
    val createdByUser = (userService.getExistingUserOrCreate(deliusUserName) as GetUserResponse.Success).user

    spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = approvedPremisesRepository.findAll().first { it.supportsSpaceBookings },
        application = null,
        offlineApplication = offlineApplication,
        placementRequest = null,
        createdBy = createdByUser,
        createdAt = OffsetDateTime.now(),
        expectedArrivalDate = spaceBookingArrivalDate,
        expectedDepartureDate = spaceBookingDepartureDate,
        actualArrivalDate = null,
        actualArrivalTime = null,
        actualDepartureDate = null,
        actualDepartureTime = null,
        canonicalArrivalDate = spaceBookingArrivalDate,
        canonicalDepartureDate = spaceBookingDepartureDate,
        crn = crn,
        keyWorkerStaffCode = null,
        keyWorkerName = null,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = null,
        cancellationRecordedAt = null,
        cancellationReason = null,
        cancellationReasonNotes = null,
        departureMoveOnCategory = null,
        departureReason = null,
        departureNotes = null,
        criteria = emptyList<CharacteristicEntity>().toMutableList(),
        nonArrivalConfirmedAt = null,
        nonArrivalNotes = null,
        nonArrivalReason = null,
        deliusEventNumber = "2",
        migratedManagementInfoFrom = null,
      ),
    )
  }

  private fun getPersonInfo(crn: String) =
    when (
      val personInfoResult = offenderService.getPersonInfoResult(
        crn = crn,
        deliusUsername = null,
        ignoreLaoRestrictions = true,
      )
    ) {
      is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
        personInfoResult.crn,
        "Offender",
      )

      is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
      is PersonInfoResult.Success.Full -> personInfoResult
    }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/cas1_application_data/$filename"

    try {
      return this::class.java.classLoader.getResource(path)?.readText() ?: ""
    } catch (e: IOException) {
      log.warn("Failed to load seed fixture $path: " + e.message!!)
      return "{}"
    }
  }
}