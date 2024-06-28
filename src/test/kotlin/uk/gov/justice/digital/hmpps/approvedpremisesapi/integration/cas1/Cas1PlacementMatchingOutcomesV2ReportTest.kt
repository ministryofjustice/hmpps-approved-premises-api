package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesV2ReportTest.Constants.REPORT_MONTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesV2ReportTest.Constants.REPORT_YEAR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import java.time.LocalDate
import java.util.UUID

class Cas1PlacementMatchingOutcomesV2ReportTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1SimpleApiClient: Cas1SimpleApiClient

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity

  lateinit var assessor: UserEntity
  lateinit var assessorJwt: String

  val standardRFPNoDecision = StandardRFPNoDecision()
  val standardRFPMatched = StandardRFPMatched()

  object Constants {
    const val REPORT_MONTH = 1
    const val REPORT_YEAR = 2020
  }

  @BeforeAll
  fun setup() {
    GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

    val assessorDetails = `Given a User`(
      roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER),
      qualifications = UserQualification.entries,
      staffUserDetailsConfigBlock = {
        withUsername("ASSESSOR1")
      },
    )
    assessor = assessorDetails.first
    assessorJwt = assessorDetails.second

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist { withDefaults() }
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist { withDefaults() }

    standardRFPNoDecision.createRequestForPlacement()
    standardRFPMatched.createRequestForPlacement()
  }

  @Test
  fun `Get report is empty if no applications`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(year = REPORT_YEAR, month = REPORT_MONTH + 1, includePii = true))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placement-matching-outcomes-2020-02-[0-9_]*.csv\"")
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
  fun `Get report returns OK with correct applications, including PII`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(year = REPORT_YEAR, month = REPORT_MONTH, includePii = true))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placement-matching-outcomes-2020-01-[0-9_]*.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<PlacementMatchingOutcomeReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(2)

          standardRFPNoDecision.assertRow(actual[0])
          standardRFPMatched.assertRow(actual[1])

          /*
          TODO:
           - placement request, no match
           - placement app
           - placement app without assessment (no match request) - shouldn't appear. maybe overkill?
           - test arrival dates outside of testing month
           - then test PII exclusions
           */

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
      assertThat(row.match_request_id).isEqualTo(application.placementRequests[0].id.toString())
      assertThat(row.matcher_cru).isNull()
      assertThat(row.matcher_username).isNull()
      assertThat(row.match_outcome).isNull()

      assertThat(row.request_for_placement_id).matches("placement_request:[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.crn).matches("StandardRFPNotAllocated")
    }
  }

  inner class StandardRFPMatched {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createSubmitAndAssessedApplication(
        crn = "StandardRFPAllocatedNoDecision",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 2),
      )
      createBooking(application.placementRequests[0].id)
    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.match_request_id).isEqualTo(application.placementRequests[0].id.toString())
      // TODO: these shouldn't be null
      assertThat(row.matcher_cru).isNull()
      assertThat(row.matcher_username).isNull()
      assertThat(row.match_outcome).isEqualTo("Placed")

      assertThat(row.request_for_placement_id).matches("placement_request:[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")
      assertThat(row.crn).matches("StandardRFPAllocatedNoDecision")
    }
  }

  private fun createBooking(
    placementRequestId: UUID,
  ) {
    val managerJwt = `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)).second

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    cas1SimpleApiClient.bookingForPlacementRequest(
      integrationTestBase = this,
      placementRequestId = placementRequestId,
      managerJwt = managerJwt,
      NewPlacementRequestBooking(
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now(),
        bedId = null,
        premisesId = premises.id,
      ),
    )

  }

  private fun createSubmitAndAssessedApplication(
    crn: String,
    arrivalDateOnApplication: LocalDate?,
  ): ApprovedPremisesApplicationEntity {
    val (applicant, jwt) = `Given a User`()
    val (offenderDetails, _) = `Given an Offender`(
      offenderDetailsConfigBlock = {
        withCrn(crn)
      },
    )

    APDeliusContext_mockSuccessfulCaseDetailCall(
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
      expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
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
    expectedArrival: LocalDate,
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
        placementDates =
        PlacementDates(
          expectedArrival = expectedArrival,
          duration = duration,
        ),
        apType = ApType.normal,
      ),
    )
  }

  private fun getLatestAssessment(applicationId: UUID) = getApplication(applicationId)
    .assessments.filter { it.reallocatedAt == null }.maxByOrNull { it.createdAt }!!

  private fun getApplication(applicationId: UUID) =
    realApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

  private fun getReportUrl(year: Int, month: Int, includePii: Boolean?) =
    "/cas1/reports/placementMatchingOutcomesV2?year=$year&month=$month" +
      if (includePii != null) { "&includePii=$includePii" } else { "" }

  data class PlacementMatchingOutcomeReportRow(
    val crn: String?,
    val request_for_placement_id: String?,
    val request_for_placement_type: String?,
    val match_request_id: String?,
    val matcher_cru: String?,
    val matcher_username: String?,
    val match_outcome: String?,
  )
}
