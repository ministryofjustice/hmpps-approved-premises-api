package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
class Cas1ApplicationV2ReportTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1SimpleApiClient: Cas1SimpleApiClient

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity

  val appSubmittedWithSuccessfulAppealsClarificationsAndWithdrawnManager = AppSubmittedWithSuccessfulAppealsClarificationsAndWithdrawnManager()
  val appSubmittedWithAcceptedAssessmentManager = AppSubmittedWithAcceptedAssessmentManager()
  val appSubmittedWithNoAssessmentManager = AppSubmittedNoAssessmentManager()
  val appWithdrawnDuringReportingPeriod = AppWithdrawnDuringReportingPeriod()
  val appSubmittedBeforeReportingPeriod = AppSubmittedBeforeReportingPeriod()
  val appSubmittedAfterReportingPeriod = AppSubmittedAfterReportingPeriod()

  @BeforeAll
  fun setup() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist { withDefaults() }
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist { withDefaults() }

    appSubmittedWithSuccessfulAppealsClarificationsAndWithdrawnManager.createApplication()
    appSubmittedWithAcceptedAssessmentManager.createApplication()
    appSubmittedWithNoAssessmentManager.createApplication()
    appWithdrawnDuringReportingPeriod.createApplication()

    appSubmittedBeforeReportingPeriod.createApplication()
    appSubmittedAfterReportingPeriod.createApplication()
  }

  @Test
  fun `Get application report is empty if no applications`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(year = 2019, month = 2, includePii = true))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"applications-2019-02-[0-9_]*.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<ApplicationReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(0)
        }
    }
  }

  @Test
  fun `Get application report returns OK with correct applications, including PII`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(year = 2020, month = 2, includePii = true))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"applications-2020-02-[0-9_]*.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<ApplicationReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(4)

          appWithdrawnDuringReportingPeriod.assertRow(actual[0])
          appSubmittedWithSuccessfulAppealsClarificationsAndWithdrawnManager.assertRow(actual[1])
          appSubmittedWithAcceptedAssessmentManager.assertRow(actual[2])
          appSubmittedWithNoAssessmentManager.assertRow(actual[3])
        }
    }
  }

  @Test
  fun `Get application report returns OK with correct applications, excludes PII by default`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(year = 2020, month = 2, includePii = null))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"applications-2020-02-[0-9_]*.csv\"")
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
            .convertTo<ApplicationReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(4)

          appSubmittedWithSuccessfulAppealsClarificationsAndWithdrawnManager.assertRow(
            row = actual[1],
            shouldIncludePii = false,
          )
        }
    }
  }

  inner class AppSubmittedWithSuccessfulAppealsClarificationsAndWithdrawnManager {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createApplication() {
      val (assessor1, assessor1Jwt) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = UserQualification.entries,
        staffDetail = StaffDetailFactory.staffDetail(
          deliusUsername = "ASSESSOR1",
          name = PersonName(forename = "Judy", middleName = "Jude", surname = "Juderson"),
        ),
        probationRegion = probationRegionEntityFactory.produceAndPersist {
          withApArea(givenAnApArea(name = "Ap Area 1"))
        },
      )

      val (assessor2, assessor2Jwt) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = UserQualification.entries,
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ASSESSOR2"),
      )

      application = createAndSubmitApplication(
        applicantDetails = givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(
            deliusUsername = "USER1",
            name = PersonName(forename = "Jeff", middleName = "Jeffity", surname = "Jefferson"),
            probationArea = ProbationArea(code = randomStringMultiCaseWithNumbers(8), description = "refRegion1"),
          ),
        ),
        nomsNumber = "noms1",
        apType = ApType.normal,
        crn = "AppSubmittedWithSuccessfulAppealsClarificationsAndWithdrawn",
        dateOfBirth = LocalDate.now().minusYears(25),
        gender = "male",
        tier = "C",
        mappaCategory = "cat1",
        mappaLevel = "level1",
        offenceId = "offenceId1",
        sentenceType = SentenceTypeOption.nonStatutory,
        releaseType = ReleaseTypeOption.licence,
        apAreaName = "apArea1",
        lduName = "ldu1",
        teamName = "refTeam1",
        targetLocation = "location1",
        submittedAt = LocalDateTime.of(2020, 2, 1, 12, 35, 0),
        timelinessCategory = Cas1ApplicationTimelinessCategory.standard,
        reasonForShortNotice = "reasonForShortNotice1",
        reasonForShortNoticeOther = "reasonForShortNoticeOther1",
        arrivalDate = LocalDate.of(2020, 5, 1),
      )
      allocateLatestAssessment(
        applicationId = application.id,
        assessor = assessor1,
        allocatedAt = LocalDateTime.of(2020, 2, 2, 13, 45, 0),
      )
      createClarificationsForLatestAssessment(
        applicationId = application.id,
        initialClarificationCreatedAt = LocalDateTime.of(2020, 3, 3, 6, 5, 30),
        initialClarificationResponseReceivedAt = LocalDate.of(2020, 3, 4),
        assessorJwt = assessor1Jwt,
      )
      updateLatestAssessment(
        applicationId = application.id,
        agreeWithShortNoticeReason = "agreeWithShortNoticeReason1",
        agreeWithShortNoticeReasonComments = "agreeWithShortNoticeReasonComments1",
        reasonForLateApplication = "reasonForLateApplication1",
        assessorJwt = assessor1Jwt,
      )
      rejectLatestAssessment(
        application = application,
        decisionDate = LocalDateTime.of(2020, 4, 1, 9, 15, 45),
        rationale = "theRejectionRationale1",
        assessorJwt = assessor1Jwt,
      )
      createAppeal(
        application = application,
        decision = AppealDecision.rejected,
        allocationDate = LocalDateTime.of(2020, 4, 5, 14, 15, 10),
        acceptanceDate = LocalDate.of(2020, 4, 6),
      )
      createAppeal(
        application = application,
        decision = AppealDecision.accepted,
        allocationDate = LocalDateTime.of(2020, 4, 7, 14, 15, 10),
        acceptanceDate = LocalDate.of(2020, 4, 8),
      )
      allocateLatestAssessment(
        applicationId = application.id,
        assessor = assessor2,
        allocatedAt = LocalDateTime.of(2020, 4, 9, 1, 0, 0),
      )
      updateLatestAssessment(
        applicationId = application.id,
        agreeWithShortNoticeReason = "agreeWithShortNoticeReason2",
        agreeWithShortNoticeReasonComments = "agreeWithShortNoticeReasonComments2",
        reasonForLateApplication = "reasonForLateApplication2",
        assessorJwt = assessor2Jwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        apType = ApType.pipe,
        decisionDate = LocalDateTime.of(2020, 5, 1, 9, 15, 45),
        assessorJwt = assessor2Jwt,
      )
      withdrawApplication(
        applicationId = application.id,
        withdrawalDate = LocalDateTime.of(2021, 12, 25, 2, 0, 0),
        reason = WithdrawalReason.duplicateApplication,
      )
    }

    fun assertRow(row: ApplicationReportRow, shouldIncludePii: Boolean = true) {
      assertThat(row.application_id).isEqualTo(application.id.toString())
      assertThat(row.crn).isEqualTo("AppSubmittedWithSuccessfulAppealsClarificationsAndWithdrawn")
      assertThat(row.noms).isEqualTo("noms1")
      assertThat(row.age_in_years).isEqualTo("25")
      assertThat(row.gender).isEqualTo("Male")
      assertThat(row.ethnicity).isEqualTo("not yet provided")
      assertThat(row.nationality).isEqualTo("not yet provided")
      assertThat(row.religion).isEqualTo("not yet provided")
      assertThat(row.has_physical_disability).isEqualTo("not yet provided")
      assertThat(row.has_learning_social_communication_difficulty).isEqualTo("not yet provided")
      assertThat(row.has_mental_health_condition).isEqualTo("not yet provided")
      assertThat(row.tier).isEqualTo("C")
      assertThat(row.mappa).isEqualTo("CAT cat1/LEVEL level1")
      assertThat(row.offence_id).isEqualTo("offenceId1")
      assertThat(row.premises_type).isEqualTo("NORMAL")
      assertThat(row.sentence_type).isEqualTo("nonStatutory")
      assertThat(row.release_type).isEqualTo("licence")
      assertThat(row.application_origin_cru).isEqualTo("apArea1")
      assertThat(row.referral_ldu).isEqualTo("ldu1")
      assertThat(row.referral_region).isEqualTo("refRegion1")
      assertThat(row.referral_team).isEqualTo("refTeam1")

      if (shouldIncludePii) {
        assertThat(row.referrer_username).isEqualTo("USER1")
        assertThat(row.referrer_name).isEqualTo("Jeff Jeffity Jefferson")
      } else {
        assertThat(row.referrer_username).isNull()
        assertThat(row.referrer_name).isNull()
      }

      assertThat(row.target_location).isEqualTo("location1")
      assertThat(row.standard_rfp_arrival_date).isEqualTo("2020-05-01")
      assertThat(row.application_submission_date).isEqualTo("2020-02-01T12:35:00Z")
      assertThat(row.application_timeliness_status).isEqualTo("standard")
      assertThat(row.applicant_reason_for_late_application).isEqualTo("reasonForShortNotice1")

      if (shouldIncludePii) {
        assertThat(row.applicant_reason_for_late_application_detail).isEqualTo("reasonForShortNoticeOther1")
      } else {
        assertThat(row.applicant_reason_for_late_application_detail).isNull()
      }

      assertThat(row.initial_assessor_agree_with_short_notice_reason).isEqualTo("agreeWithShortNoticeReason1")

      if (shouldIncludePii) {
        assertThat(row.initial_assessor_reason_for_late_application).isEqualTo("agreeWithShortNoticeReasonComments1")
      } else {
        assertThat(row.initial_assessor_reason_for_late_application).isNull()
      }

      assertThat(row.initial_assessor_reason_for_late_application_detail).isEqualTo("reasonForLateApplication1")
      assertThat(row.initial_assessor_premises_type).isNull()
      assertThat(row.last_allocated_to_initial_assessor_date).isEqualTo("2020-02-02T13:45:00Z")
      assertThat(row.initial_assessor_cru).isEqualTo("Ap Area 1")

      if (shouldIncludePii) {
        assertThat(row.initial_assessor_username).isEqualTo("ASSESSOR1")
        assertThat(row.initial_assessor_name).isEqualTo("Judy Jude Juderson")
      } else {
        assertThat(row.initial_assessor_username).isNull()
        assertThat(row.initial_assessor_name).isNull()
      }

      assertThat(row.initial_assessment_further_information_requested_on).isEqualTo("2020-03-03T06:05:30Z")
      assertThat(row.initial_assessment_further_information_received_at).isEqualTo("2020-03-04")
      assertThat(row.initial_assessment_decision_date).isEqualTo("2020-04-01T09:15:45Z")
      assertThat(row.initial_assessment_decision).isEqualTo("REJECTED")
      assertThat(row.initial_assessment_decision_rationale).isEqualTo("theRejectionRationale1")
      assertThat(row.assessment_appeal_count).isEqualTo("2")
      assertThat(row.last_appealed_assessment_decision).isEqualTo("accepted")
      assertThat(row.last_appealed_assessment_date).isEqualTo("2020-04-08")
      assertThat(row.last_allocated_to_appealed_assessor_date).isEqualTo("2020-04-09T01:00:00Z")
      assertThat(row.last_allocated_to_appealed_assessor_premises_type).isEqualTo("PIPE")

      if (shouldIncludePii) {
        assertThat(row.last_appealed_assessor_username).isEqualTo("ASSESSOR2")
      } else {
        assertThat(row.last_appealed_assessor_username).isNull()
      }

      assertThat(row.application_withdrawal_date).isEqualTo("2021-12-25T02:00:00Z")
      assertThat(row.application_withdrawal_reason).isEqualTo("duplicate_application")
    }
  }

  inner class AppSubmittedWithAcceptedAssessmentManager {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createApplication() {
      val (assessor4, assessor4jwt) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = UserQualification.entries,
        staffDetail = StaffDetailFactory.staffDetail(
          deliusUsername = "ASSESSOR4",
          name = PersonName(forename = "Assessor", surname = "Assessing"),
        ),
        probationRegion = probationRegionEntityFactory.produceAndPersist {
          withApArea(givenAnApArea(name = "Ap Area 4"))
        },
      )

      application = createAndSubmitApplication(
        applicantDetails = givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(
            deliusUsername = "USER3",
            name = PersonName(forename = "Test", surname = "Testing"),
            probationArea = ProbationArea(code = randomStringMultiCaseWithNumbers(8), description = "refRegion3"),
          ),
        ),
        nomsNumber = "noms3",
        apType = ApType.pipe,
        crn = "AppSubmittedWithAcceptedAssessment",
        dateOfBirth = LocalDate.now().minusYears(50),
        gender = "female",
        tier = "X",
        mappaCategory = "cat3",
        mappaLevel = "level3",
        offenceId = "offenceId3",
        sentenceType = SentenceTypeOption.bailPlacement,
        releaseType = ReleaseTypeOption.rotl,
        apAreaName = "apArea3",
        lduName = "ldu3",
        teamName = "refTeam3",
        targetLocation = "location3",
        submittedAt = LocalDateTime.of(2020, 2, 15, 11, 25, 0),
        timelinessCategory = Cas1ApplicationTimelinessCategory.emergency,
        reasonForShortNotice = "reasonForShortNotice3",
        reasonForShortNoticeOther = "reasonForShortNoticeOther3",
        arrivalDate = LocalDate.of(2030, 12, 31),
      )
      allocateLatestAssessment(
        applicationId = application.id,
        assessor = assessor4,
        allocatedAt = LocalDateTime.of(2023, 9, 11, 3, 25, 10),
      )
      updateLatestAssessment(
        applicationId = application.id,
        agreeWithShortNoticeReason = "agreeWithShortNoticeReason4",
        agreeWithShortNoticeReasonComments = "agreeWithShortNoticeReasonComments4",
        reasonForLateApplication = "reasonForLateApplication4",
        assessorJwt = assessor4jwt,
      )
      acceptLatestAssessment(
        applicationId = application.id,
        apType = ApType.mhapStJosephs,
        decisionDate = LocalDateTime.of(2020, 12, 1, 9, 15, 45),
        assessorJwt = assessor4jwt,
      )
    }

    fun assertRow(row: ApplicationReportRow) {
      assertThat(row.application_id).isEqualTo(application.id.toString())
      assertThat(row.crn).isEqualTo("AppSubmittedWithAcceptedAssessment")
      assertThat(row.noms).isEqualTo("noms3")
      assertThat(row.age_in_years).isEqualTo("50")
      assertThat(row.gender).isEqualTo("Female")
      assertThat(row.ethnicity).isEqualTo("not yet provided")
      assertThat(row.nationality).isEqualTo("not yet provided")
      assertThat(row.religion).isEqualTo("not yet provided")
      assertThat(row.has_physical_disability).isEqualTo("not yet provided")
      assertThat(row.has_learning_social_communication_difficulty).isEqualTo("not yet provided")
      assertThat(row.has_mental_health_condition).isEqualTo("not yet provided")
      assertThat(row.tier).isEqualTo("X")
      assertThat(row.mappa).isEqualTo("CAT cat3/LEVEL level3")
      assertThat(row.offence_id).isEqualTo("offenceId3")
      assertThat(row.premises_type).isEqualTo("PIPE")
      assertThat(row.sentence_type).isEqualTo("bailPlacement")
      assertThat(row.release_type).isEqualTo("rotl")
      assertThat(row.application_origin_cru).isEqualTo("apArea3")
      assertThat(row.referral_ldu).isEqualTo("ldu3")
      assertThat(row.referral_region).isEqualTo("refRegion3")
      assertThat(row.referral_team).isEqualTo("refTeam3")
      assertThat(row.referrer_username).isEqualTo("USER3")
      assertThat(row.referrer_name).isEqualTo("Test Testing")
      assertThat(row.target_location).isEqualTo("location3")
      assertThat(row.standard_rfp_arrival_date).isEqualTo("2030-12-31")
      assertThat(row.application_submission_date).isEqualTo("2020-02-15T11:25:00Z")
      assertThat(row.application_timeliness_status).isEqualTo("emergency")
      assertThat(row.applicant_reason_for_late_application).isEqualTo("reasonForShortNotice3")
      assertThat(row.applicant_reason_for_late_application_detail).isEqualTo("reasonForShortNoticeOther3")

      assertThat(row.initial_assessor_agree_with_short_notice_reason).isEqualTo("agreeWithShortNoticeReason4")
      assertThat(row.initial_assessor_reason_for_late_application).isEqualTo("agreeWithShortNoticeReasonComments4")
      assertThat(row.initial_assessor_reason_for_late_application_detail).isEqualTo("reasonForLateApplication4")
      assertThat(row.initial_assessor_premises_type).isEqualTo("MHAP_ST_JOSEPHS")
      assertThat(row.last_allocated_to_initial_assessor_date).isEqualTo("2023-09-11T03:25:10Z")
      assertThat(row.initial_assessor_cru).isEqualTo("Ap Area 4")
      assertThat(row.initial_assessor_username).isEqualTo("ASSESSOR4")
      assertThat(row.initial_assessor_name).isEqualTo("Assessor Assessing")
      assertThat(row.initial_assessment_further_information_requested_on).isNull()
      assertThat(row.initial_assessment_further_information_received_at).isNull()
      assertThat(row.initial_assessment_decision_date).isEqualTo("2020-12-01T09:15:45Z")
      assertThat(row.initial_assessment_decision).isEqualTo("ACCEPTED")
      assertThat(row.initial_assessment_decision_rationale).isNull()
      assertThat(row.assessment_appeal_count).isEqualTo("0")
      assertThat(row.last_appealed_assessment_decision).isNull()
      assertThat(row.last_appealed_assessment_date).isNull()
      assertThat(row.last_allocated_to_appealed_assessor_date).isNull()
      assertThat(row.last_allocated_to_appealed_assessor_premises_type).isNull()
      assertThat(row.last_appealed_assessor_username).isNull()

      assertThat(row.application_withdrawal_date).isNull()
      assertThat(row.application_withdrawal_reason).isNull()
    }
  }

  inner class AppSubmittedNoAssessmentManager {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createApplication() {
      val (assessor3, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = UserQualification.entries,
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ASSESSOR3"),
      )

      application = createAndSubmitApplication(
        applicantDetails = givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(
            deliusUsername = "USER2",
            name = PersonName(forename = "App", surname = "Licant"),
            probationArea = ProbationArea(code = randomStringMultiCaseWithNumbers(8), description = "refRegion2"),
          ),
        ),
        nomsNumber = "noms2",
        apType = ApType.esap,
        crn = "AppSubmittedNoAssessment",
        dateOfBirth = LocalDate.now().minusYears(20),
        gender = "female",
        tier = "B",
        mappaCategory = "cat2",
        mappaLevel = "level2",
        offenceId = "offenceId2",
        sentenceType = SentenceTypeOption.ipp,
        releaseType = ReleaseTypeOption.notApplicable,
        apAreaName = "apArea2",
        lduName = "ldu2",
        teamName = "refTeam2",
        targetLocation = "location2",
        submittedAt = LocalDateTime.of(2020, 2, 29, 11, 25, 0),
        timelinessCategory = Cas1ApplicationTimelinessCategory.shortNotice,
        reasonForShortNotice = null,
        reasonForShortNoticeOther = null,
        arrivalDate = null,
      )
      allocateLatestAssessment(
        applicationId = application.id,
        assessor = assessor3,
        allocatedAt = LocalDateTime.of(2023, 8, 11, 3, 25, 10),
      )
    }

    fun assertRow(row: ApplicationReportRow) {
      assertThat(row.application_id).isEqualTo(application.id.toString())
      assertThat(row.crn).isEqualTo("AppSubmittedNoAssessment")
      assertThat(row.noms).isEqualTo("noms2")
      assertThat(row.age_in_years).isEqualTo("20")
      assertThat(row.gender).isEqualTo("Female")
      assertThat(row.ethnicity).isEqualTo("not yet provided")
      assertThat(row.nationality).isEqualTo("not yet provided")
      assertThat(row.religion).isEqualTo("not yet provided")
      assertThat(row.has_physical_disability).isEqualTo("not yet provided")
      assertThat(row.has_learning_social_communication_difficulty).isEqualTo("not yet provided")
      assertThat(row.has_mental_health_condition).isEqualTo("not yet provided")
      assertThat(row.tier).isEqualTo("B")
      assertThat(row.mappa).isEqualTo("CAT cat2/LEVEL level2")
      assertThat(row.offence_id).isEqualTo("offenceId2")
      assertThat(row.premises_type).isEqualTo("ESAP")
      assertThat(row.sentence_type).isEqualTo("ipp")
      assertThat(row.release_type).isEqualTo("notApplicable")
      assertThat(row.application_origin_cru).isEqualTo("apArea2")
      assertThat(row.referral_ldu).isEqualTo("ldu2")
      assertThat(row.referral_region).isEqualTo("refRegion2")
      assertThat(row.referral_team).isEqualTo("refTeam2")
      assertThat(row.referrer_username).isEqualTo("USER2")
      assertThat(row.referrer_name).isEqualTo("App Licant")
      assertThat(row.target_location).isEqualTo("location2")
      assertThat(row.standard_rfp_arrival_date).isNull()
      assertThat(row.application_submission_date).isEqualTo("2020-02-29T11:25:00Z")
      assertThat(row.application_timeliness_status).isEqualTo("shortNotice")
      assertThat(row.applicant_reason_for_late_application).isNull()
      assertThat(row.applicant_reason_for_late_application_detail).isNull()

      assertThat(row.initial_assessor_agree_with_short_notice_reason).isNull()
      assertThat(row.initial_assessor_reason_for_late_application).isNull()
      assertThat(row.initial_assessor_reason_for_late_application_detail).isNull()
      assertThat(row.initial_assessor_premises_type).isNull()
      assertThat(row.last_allocated_to_initial_assessor_date).isEqualTo("2023-08-11T03:25:10Z")
      assertThat(row.initial_assessor_cru).isNull()
      assertThat(row.initial_assessor_username).isNull()
      assertThat(row.initial_assessment_further_information_requested_on).isNull()
      assertThat(row.initial_assessment_further_information_received_at).isNull()
      assertThat(row.initial_assessment_decision_date).isNull()
      assertThat(row.initial_assessment_decision).isNull()
      assertThat(row.initial_assessment_decision_rationale).isNull()
      assertThat(row.assessment_appeal_count).isEqualTo("0")
      assertThat(row.last_appealed_assessment_decision).isNull()
      assertThat(row.last_appealed_assessment_date).isNull()
      assertThat(row.last_allocated_to_appealed_assessor_date).isNull()
      assertThat(row.last_allocated_to_appealed_assessor_premises_type).isNull()
      assertThat(row.last_appealed_assessor_username).isNull()

      assertThat(row.application_withdrawal_date).isNull()
      assertThat(row.application_withdrawal_reason).isNull()
    }
  }

  inner class AppWithdrawnDuringReportingPeriod {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createApplication() {
      application = createAndSubmitApplication(
        applicantDetails = givenAUser(),
        nomsNumber = "noms10",
        apType = ApType.esap,
        crn = "appWithdrawnDuringReportingPeriod",
        dateOfBirth = LocalDate.now().minusYears(20),
        gender = "female",
        tier = "B",
        mappaCategory = "cat2",
        mappaLevel = "level2",
        offenceId = "offenceId2",
        sentenceType = SentenceTypeOption.ipp,
        releaseType = ReleaseTypeOption.notApplicable,
        apAreaName = "apArea2",
        lduName = "ldu2",
        teamName = "refTeam2",
        targetLocation = "location2",
        submittedAt = LocalDateTime.of(2020, 1, 30, 11, 25, 0),
        timelinessCategory = Cas1ApplicationTimelinessCategory.shortNotice,
        reasonForShortNotice = null,
        reasonForShortNoticeOther = null,
        arrivalDate = null,
      )
      withdrawApplication(
        applicationId = application.id,
        withdrawalDate = LocalDateTime.of(2020, 2, 12, 11, 25, 0),
        WithdrawalReason.death,
      )
    }

    fun assertRow(row: ApplicationReportRow) {
      assertThat(row.application_id).isEqualTo(application.id.toString())
      assertThat(row.crn).isEqualTo("appWithdrawnDuringReportingPeriod")

      assertThat(row.application_withdrawal_date).isEqualTo("2020-02-12T11:25:00Z")
      assertThat(row.application_withdrawal_reason).isEqualTo("death")
    }
  }

  inner class AppSubmittedBeforeReportingPeriod {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createApplication() {
      application = createAndSubmitApplication(
        applicantDetails = givenAUser(),
        nomsNumber = "noms2",
        apType = ApType.esap,
        crn = "submittedAppBeforeReportDate",
        dateOfBirth = LocalDate.now().minusYears(20),
        gender = "female",
        tier = "B",
        mappaCategory = "cat2",
        mappaLevel = "level2",
        offenceId = "offenceId2",
        sentenceType = SentenceTypeOption.ipp,
        releaseType = ReleaseTypeOption.notApplicable,
        apAreaName = "apArea2",
        lduName = "ldu2",
        teamName = "refTeam2",
        targetLocation = "location2",
        submittedAt = LocalDateTime.of(2020, 1, 30, 11, 25, 0),
        timelinessCategory = Cas1ApplicationTimelinessCategory.shortNotice,
        reasonForShortNotice = null,
        reasonForShortNoticeOther = null,
        arrivalDate = null,
      )
    }
  }

  inner class AppSubmittedAfterReportingPeriod {
    lateinit var application: ApprovedPremisesApplicationEntity

    fun createApplication() {
      application = createAndSubmitApplication(
        applicantDetails = givenAUser(),
        nomsNumber = "noms2",
        apType = ApType.esap,
        crn = "submittedAppBeforeReportDate",
        dateOfBirth = LocalDate.now().minusYears(20),
        gender = "female",
        tier = "B",
        mappaCategory = "cat2",
        mappaLevel = "level2",
        offenceId = "offenceId2",
        sentenceType = SentenceTypeOption.ipp,
        releaseType = ReleaseTypeOption.notApplicable,
        apAreaName = "apArea2",
        lduName = "ldu2",
        teamName = "refTeam2",
        targetLocation = "location2",
        submittedAt = LocalDateTime.of(2020, 3, 1, 1, 0, 0),
        timelinessCategory = Cas1ApplicationTimelinessCategory.shortNotice,
        reasonForShortNotice = null,
        reasonForShortNoticeOther = null,
        arrivalDate = null,
      )
    }
  }

  @SuppressWarnings("ConstructorParameterNaming")
  data class ApplicationReportRow(
    val application_id: String,
    val crn: String,
    val noms: String?,
    val age_in_years: String?,
    val gender: String?,
    val ethnicity: String,
    val nationality: String,
    val religion: String,
    val has_physical_disability: String,
    val has_learning_social_communication_difficulty: String,
    val has_mental_health_condition: String,
    val tier: String?,
    val mappa: String?,
    val offence_id: String?,
    val premises_type: String?,
    val sentence_type: String?,
    val release_type: String?,
    val application_origin_cru: String?,
    val referral_ldu: String?,
    val referral_region: String?,
    val referral_team: String?,
    val referrer_username: String?,
    val referrer_name: String?,
    val standard_rfp_arrival_date: String?,
    val target_location: String?,
    val application_submission_date: String?,
    val application_timeliness_status: String?,
    val applicant_reason_for_late_application: String?,
    val applicant_reason_for_late_application_detail: String?,
    val initial_assessor_agree_with_short_notice_reason: String?,
    val initial_assessor_reason_for_late_application: String?,
    val initial_assessor_reason_for_late_application_detail: String?,
    val initial_assessor_premises_type: String?,
    val last_allocated_to_initial_assessor_date: String?,
    val initial_assessor_cru: String?,
    val initial_assessor_username: String?,
    val initial_assessor_name: String?,
    val initial_assessment_further_information_requested_on: String?,
    val initial_assessment_further_information_received_at: String?,
    val initial_assessment_decision_date: String?,
    val initial_assessment_decision: String?,
    val initial_assessment_decision_rationale: String?,
    val assessment_appeal_count: String?,
    val last_appealed_assessment_decision: String?,
    val last_appealed_assessment_date: String?,
    val last_allocated_to_appealed_assessor_date: String?,
    val last_allocated_to_appealed_assessor_premises_type: String?,
    val last_appealed_assessor_username: String?,
    val application_withdrawal_date: String?,
    val application_withdrawal_reason: String?,
  )

  private fun createAndSubmitApplication(
    applicantDetails: Pair<UserEntity, String>,
    nomsNumber: String,
    apType: ApType,
    crn: String,
    dateOfBirth: LocalDate,
    gender: String,
    tier: String,
    mappaCategory: String,
    mappaLevel: String,
    offenceId: String,
    sentenceType: SentenceTypeOption,
    releaseType: ReleaseTypeOption,
    apAreaName: String,
    lduName: String,
    teamName: String,
    targetLocation: String,
    submittedAt: LocalDateTime,
    timelinessCategory: Cas1ApplicationTimelinessCategory,
    reasonForShortNotice: String?,
    reasonForShortNoticeOther: String?,
    arrivalDate: LocalDate?,
  ): ApprovedPremisesApplicationEntity {
    val (applicant, jwt) = applicantDetails
    val (offenderDetails, _) = givenAnOffender(
      offenderDetailsConfigBlock = {
        withCrn(crn)
        withDateOfBirth(dateOfBirth)
        withGender(gender)
        withNomsNumber(nomsNumber)
      },
    )

    apDeliusContextMockSuccessfulCaseDetailCall(
      offenderDetails.otherIds.crn,
      CaseDetailFactory()
        .from(offenderDetails.asCaseDetail())
        .withMappaDetail(
          MappaDetail(
            level = 59,
            levelDescription = mappaLevel,
            category = 2,
            categoryDescription = mappaCategory,
            startDate = LocalDate.now(),
            lastUpdated = ZonedDateTime.now(),
          ),
        )
        .withCase(
          CaseSummaryFactory()
            .withManager(
              ManagerFactory()
                .withTeam(
                  TeamFactoryDeliusContext.team(
                    name = teamName,
                    ldu = Ldu(code = randomStringMultiCaseWithNumbers(10), name = lduName),
                  ),
                )
                .produce(),
            )
            .produce(),
        )
        .produce(),
    )

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(applicant)
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
      withApplicationSchema(applicationSchema)
      withData("{}")
      withOffenceId(offenceId)
      withRiskRatings(
        PersonRisksFactory()
          .withTier(
            RiskWithStatus(
              status = RiskStatus.Retrieved,
              value = RiskTier(
                level = tier,
                lastUpdated = LocalDate.now(),
              ),
            ),
          ).produce(),
      )
    }

    clock.setNow(submittedAt)

    val apArea = givenAnApArea(name = apAreaName)

    cas1SimpleApiClient.applicationSubmit(
      this,
      application.id,
      jwt,
      SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = targetLocation,
        releaseType = releaseType,
        type = "CAS1",
        sentenceType = sentenceType,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
        caseManagerIsNotApplicant = false,
        reasonForShortNotice = reasonForShortNotice,
        reasonForShortNoticeOther = reasonForShortNoticeOther,
        apType = apType,
        noticeType = timelinessCategory,
        apAreaId = apArea.id,
        arrivalDate = arrivalDate,
      ),
    )

    return realApplicationRepository.findByIdOrNull(application.id) as ApprovedPremisesApplicationEntity
  }

  private fun createClarificationsForLatestAssessment(
    applicationId: UUID,
    assessorJwt: String,
    initialClarificationCreatedAt: LocalDateTime,
    initialClarificationResponseReceivedAt: LocalDate,
  ) {
    val assessmentId = getLatestAssessment(applicationId).id

    clock.setNow(initialClarificationCreatedAt)

    val initialNote = cas1SimpleApiClient.assessmentCreateClarificationNote(
      this,
      assessmentId,
      assessorJwt,
      NewClarificationNote(query = "This is the initial query"),
    )

    cas1SimpleApiClient.assessmentUpdateClarificationNote(
      this,
      assessmentId,
      initialNote.id,
      assessorJwt,
      UpdatedClarificationNote(
        response = "This is the initial response",
        responseReceivedOn = initialClarificationResponseReceivedAt!!,
      ),
    )

    clock.setNow(initialClarificationCreatedAt.plusDays(12))

    val subsequentNote = cas1SimpleApiClient.assessmentCreateClarificationNote(
      this,
      assessmentId,
      assessorJwt,
      NewClarificationNote(query = "This is an additional query that should be ignored in the report"),
    )

    cas1SimpleApiClient.assessmentUpdateClarificationNote(
      this,
      assessmentId,
      subsequentNote.id,
      assessorJwt,
      UpdatedClarificationNote(
        response = "This is the subsequent response",
        responseReceivedOn = LocalDate.now(),
      ),
    )
  }

  private fun allocateLatestAssessment(
    applicationId: UUID,
    allocatedAt: LocalDateTime,
    assessor: UserEntity,
  ) {
    clock.setNow(allocatedAt)

    cas1SimpleApiClient.assessmentReallocate(
      this,
      realApplicationRepository.findByIdOrNull(applicationId)!!.getLatestAssessment()!!.id,
      assessor.id,
    )
  }

  fun updateLatestAssessment(
    applicationId: UUID,
    assessorJwt: String,
    agreeWithShortNoticeReason: String,
    agreeWithShortNoticeReasonComments: String,
    reasonForLateApplication: String,
  ) {
    val assessmentId = getLatestAssessment(applicationId).id

    cas1SimpleApiClient.assessmentUpdate(
      this,
      assessmentId,
      assessorJwt,
      UpdateAssessment(
        data = mapOf(
          "suitability-assessment" to mapOf(
            "application-timeliness" to mapOf(
              "agreeWithShortNoticeReason" to agreeWithShortNoticeReason,
              "agreeWithShortNoticeReasonComments" to agreeWithShortNoticeReasonComments,
              "reasonForLateApplication" to reasonForLateApplication,
            ),
          ),
        ),
      ),
    )
  }

  private fun acceptLatestAssessment(
    applicationId: UUID,
    apType: ApType,
    decisionDate: LocalDateTime,
    assessorJwt: String,
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

    clock.setNow(decisionDate)

    cas1SimpleApiClient.assessmentAccept(
      this,
      assessmentId,
      assessorJwt,
      AssessmentAcceptance(
        document = mapOf("document" to "value"),
        requirements = placementRequirements,
        placementDates = null,
        apType = apType,
      ),
    )
  }

  private fun rejectLatestAssessment(
    application: ApprovedPremisesApplicationEntity,
    decisionDate: LocalDateTime,
    rationale: String,
    assessorJwt: String,
  ) {
    val assessmentId = getLatestAssessment(application.id).id

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

  private fun createAppeal(
    application: ApprovedPremisesApplicationEntity,
    decision: AppealDecision,
    allocationDate: LocalDateTime,
    acceptanceDate: LocalDate,
  ) {
    clock.setNow(allocationDate)

    cas1SimpleApiClient.assessmentAppealCreate(
      this,
      application.id,
      NewAppeal(
        appealDate = acceptanceDate,
        appealDetail = "the detail",
        decision = decision,
        decisionDetail = "the decision detail",
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
      NewWithdrawal(
        reason,
      ),
    )
  }

  private fun getLatestAssessment(applicationId: UUID) = getApplication(applicationId).getLatestAssessment()!!

  private fun getApplication(applicationId: UUID) =
    realApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

  private fun getReportUrl(year: Int, month: Int, includePii: Boolean?) =
    "/cas1/reports/applicationsV2?year=$year&month=$month" +
      if (includePii != null) {
        "&includePii=$includePii"
      } else {
        ""
      }
}
