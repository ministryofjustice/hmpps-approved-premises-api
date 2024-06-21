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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1ApplicationReportTest.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesV2ReportTest.Constants.REPORT_MONTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesV2ReportTest.Constants.REPORT_YEAR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1RequestForPlacementReportTest.RequestForPlacementReportRow
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
import java.time.LocalDateTime
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

  val standardRFPNotAssessed = StandardRFPNotAllocated()

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

    standardRFPNotAssessed.createRequestForPlacement()
  }

  @Test
  fun `Get report is empty if no applications`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(year = 2019, month = 2, includePii = true))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placement-matching-outcomes-2019-02-[0-9_]*.csv\"")
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

          assertThat(actual.size).isEqualTo(1)

          standardRFPNotAssessed.assertRow(actual[0])

          /*
          TODO:
           - placement request, assigned and matched
           - placement request, assigned, not matched
           - placement app
           - test arrival dates outside of testing range
           - test that withdrawals are not included
           - then test additional report columns
           - then test PII exclusions
           */

          // TODO: The report says only show complete, not withdrawn.
          // Note that eventually unmatched are typically withdrawn
          // so we may need to improve how we track this, or show withdrawn
          // where the withdrawn reason is linked to matching?
        }
    }
  }

  inner class StandardRFPNotAllocated {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createRequestForPlacement() {
      application = createSubmitAndAssessApplication(
        crn = "StandardRFPNotAssessed",
        arrivalDateOnApplication = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
      )

    }

    fun assertRow(row: PlacementMatchingOutcomeReportRow) {
      assertThat(row.request_for_placement_id).matches("placement_request:[a-f0-9-]+")
      assertThat(row.request_for_placement_type).isEqualTo("STANDARD")

      // TODO: test all columns unique to the report
    }
  }

  private fun createSubmitAndAssessApplication(
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
    val request_for_placement_id: String?,
    val request_for_placement_type: String?,
  )
}
