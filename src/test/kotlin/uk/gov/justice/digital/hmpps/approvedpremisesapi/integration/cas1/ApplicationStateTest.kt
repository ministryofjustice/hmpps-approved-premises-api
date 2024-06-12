package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationStateTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  lateinit var offenderDetails: OffenderDetailSummary

  lateinit var jwt: String

  lateinit var user: UserEntity

  lateinit var applicationId: UUID

  @BeforeEach
  fun setup() {
    val (offenderDetails) = `Given an Offender`()
    val (user, jwt) = `Given a User`(
      roles = listOf(
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_MATCHER,
        UserRole.CAS1_WORKFLOW_MANAGER,
      ),
    )
    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withPermissiveSchema()
    }

    APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
      offenderDetails.otherIds.crn,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAM1"),
      ),
    )

    APOASysContext_mockSuccessfulNeedsDetailsCall(
      offenderDetails.otherIds.crn,
      NeedsDetailsFactory().produce(),
    )

    GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

    this.offenderDetails = offenderDetails
    this.user = user
    this.jwt = jwt
    this.applicationId = createApplication()
  }

  @Test
  fun `a CAS1 application can transition between all states correctly`() {
    assertApplicationStatus(ApprovedPremisesApplicationStatus.STARTED)

    submitApplication(true)
    assertApplicationStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)

    startAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)

    approveAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)

    createBooking()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
  }

  @Test
  fun `a CAS1 application can transition between all states correctly with no arrival date`() {
    assertApplicationStatus(ApprovedPremisesApplicationStatus.STARTED)

    submitApplication(false)
    assertApplicationStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)

    startAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)

    approveAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

    createBooking()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
  }

  @Test
  fun `a CAS1 application status changes correctly when an assessment gets reallocated`() {
    submitApplication()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)

    reallocateAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
  }

  @Test
  fun `a CAS1 application can transition to a rejected state`() {
    submitApplication()
    startAssessment()
    rejectAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.REJECTED)
  }

  @Test
  fun `a CAS1 application can set an inapplicable state correctly`() {
    setApplicationToInapplicable()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.INAPPLICABLE)
  }

  @Test
  fun `a CAS1 application can set an withdrawn state correctly`() {
    withdrawApplication()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.WITHDRAWN)
  }

  @Test
  fun `a CAS1 application can set the clarification notes status correctly and reset when the note has been completed`() {
    submitApplication()
    startAssessment()

    requestFurtherInformation()
    updateAssessment()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)

    updateFurtherInformation()
    assertApplicationStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
  }

  private fun assertApplicationStatus(status: ApprovedPremisesApplicationStatus) {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    assertThat(application.status).isEqualTo(status)
  }

  private fun requestFurtherInformation() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!

    webTestClient.post()
      .uri("/assessments/${assessment.id}/notes")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewClarificationNote(
          query = "some text",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun updateFurtherInformation() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!

    val clarificationNote = assessment.clarificationNotes[0]

    webTestClient.put()
      .uri("/assessments/${assessment.id}/notes/${clarificationNote.id}")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdatedClarificationNote(
          response = "some text",
          responseReceivedOn = LocalDate.parse("2022-03-04"),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun rejectAssessment() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!

    webTestClient.post()
      .uri("/assessments/${assessment.id}/rejection")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(AssessmentRejection(document = mapOf("document" to "value"), rejectionRationale = "reasoning"))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createBooking() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val placementRequest = application.getLatestPlacementRequest()!!

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
      .uri("/placement-requests/${placementRequest.id}/booking")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPlacementRequestBooking(
          arrivalDate = LocalDate.parse("2023-03-29"),
          departureDate = LocalDate.parse("2023-04-01"),
          bedId = bed.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun startAssessment() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!

    webTestClient.put()
      .uri("/assessments/${assessment.id}")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateAssessment(
          data = mapOf("some text" to 5),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun updateAssessment() = startAssessment()

  private fun approveAssessment() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!
    val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

    val placementRequirements = PlacementRequirements(
      gender = Gender.male,
      type = ApType.normal,
      location = postcodeDistrict.outcode,
      radius = 50,
      essentialCriteria = listOf(),
      desirableCriteria = listOf(),
    )

    assessment.schemaUpToDate = true

    webTestClient.post()
      .uri("/assessments/${assessment.id}/acceptance")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        AssessmentAcceptance(
          document = mapOf("document" to "value"),
          requirements = placementRequirements,
          notes = "Some Notes",
          placementDates = PlacementDates(
            expectedArrival = LocalDate.now(),
            duration = 12,
          ),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createApplication(): UUID {
    val result = webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApplication(
          crn = offenderDetails.otherIds.crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = "789",
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ApprovedPremisesApplication::class.java)

    val applicationResponse = result.responseBody.blockFirst() as ApprovedPremisesApplication
    return applicationResponse.id
  }

  private fun setApplicationToInapplicable() {
    webTestClient.put()
      .uri("/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateApprovedPremisesApplication(
          data = mapOf("thingId" to 123),
          isWomensApplication = false,
          isPipeApplication = true,
          type = UpdateApplicationType.CAS1,
          isInapplicable = true,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun withdrawApplication() {
    webTestClient.post()
      .uri("/applications/$applicationId/withdrawal")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewWithdrawal(
          reason = WithdrawalReason.duplicateApplication,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun submitApplication(hasArrival: Boolean = true) {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    application.data = "{\"data\": \"something\"}"
    val arrivalDate = if (hasArrival) {
      LocalDate.now().plusMonths(7)
    } else {
      null
    }

    realApplicationRepository.save(application)

    webTestClient.post()
      .uri("/applications/$applicationId/submission")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        SubmitApprovedPremisesApplication(
          translatedDocument = {},
          isPipeApplication = false,
          isWomensApplication = false,
          isEmergencyApplication = false,
          isEsapApplication = false,
          targetLocation = "SW1A 1AA",
          releaseType = ReleaseTypeOption.licence,
          type = "CAS1",
          arrivalDate = arrivalDate,
          sentenceType = SentenceTypeOption.nonStatutory,
          applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
          caseManagerIsNotApplicant = false,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun reallocateAssessment() {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!

    webTestClient.post()
      .uri("/tasks/assessment/${assessment.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(
        NewReallocation(
          userId = user.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
  }
}
