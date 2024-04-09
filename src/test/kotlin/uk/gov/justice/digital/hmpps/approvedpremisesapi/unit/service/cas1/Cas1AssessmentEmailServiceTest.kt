package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.DEFAULT_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.NEXT_WORKING_DAY_EMERGENCY_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.SAME_DAY_EMERGENCY_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.STANDARD_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1AssessmentEmailServiceTest.Constants.ALLOCATED_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1AssessmentEmailServiceTest.Constants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentEmailServiceTest {

  object Constants {
    const val ALLOCATED_EMAIL = "applicant@test.com"
    const val CRN = "CRN123"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  val service = createService(aps530WithdrawalEmailImprovements = false)
  val serviceUsingAps530Improvements = createService(aps530WithdrawalEmailImprovements = true)

  private fun createService(aps530WithdrawalEmailImprovements: Boolean) = Cas1AssessmentEmailService(
    mockEmailNotificationService,
    notifyConfig,
    assessmentUrlTemplate = UrlTemplate("http://frontend/assessments/#id"),
    applicationUrlTemplate = UrlTemplate("http://frontend/application/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/application/#applicationId?tab=timeline"),
    workingDayService = mockWorkingDayService,
    aps530WithdrawalEmailImprovements = aps530WithdrawalEmailImprovements,
  )

  @BeforeEach
  fun beforeEach() {
    mockEmailNotificationService.reset()
  }

  @Nested
  inner class AssessmentAllocated {
    private val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(ALLOCATED_EMAIL)
      .produce()
    private val assessmentID = UUID.randomUUID()

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and no deadline`() {
      service.assessmentAllocated(applicant, assessmentID, CRN, null, false)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(
          applicant.name,
          CRN,
          assessmentID,
          DEFAULT_DEADLINE_COPY,
        ),
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and an emergency assessment with a deadline of today`() {
      val deadline = OffsetDateTime.now().minusHours(2)
      service.assessmentAllocated(applicant, assessmentID, CRN, deadline, true)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(
          applicant.name,
          CRN,
          assessmentID,
          SAME_DAY_EMERGENCY_DEADLINE_COPY,
        ),
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and an emergency assessment with a deadline of next working day`() {
      every { mockWorkingDayService.getCompleteWorkingDaysFromNowUntil(any()) } returns 2
      val deadline = OffsetDateTime.now().plusDays(2)

      service.assessmentAllocated(applicant, assessmentID, CRN, deadline, true)

      val expectedDeadlineCopy = NEXT_WORKING_DAY_EMERGENCY_DEADLINE_COPY.format(deadline.toUiFormattedHourOfDay(), deadline.toLocalDate().toUiFormat())

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(
          applicant.name,
          CRN,
          assessmentID,
          expectedDeadlineCopy,
        ),
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and a standard deadline`() {
      every { mockWorkingDayService.getCompleteWorkingDaysFromNowUntil(any()) } returns 10
      val deadline = OffsetDateTime.now().plusDays(10)

      service.assessmentAllocated(applicant, assessmentID, CRN, deadline, false)

      val expectedDeadlineCopy = STANDARD_DEADLINE_COPY.format("10")

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(
          applicant.name,
          CRN,
          assessmentID,
          expectedDeadlineCopy,
        ),
      )
    }

    @Test
    fun `assessmentAllocated does not send an email to a user if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()
      val assessmentID = UUID.randomUUID()

      service.assessmentAllocated(
        applicant,
        assessmentID,
        CRN,
        OffsetDateTime.now(),
        false,
      )
      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    private fun expectedAssessmentAllocatedPersonalisation(applicantName: String, crn: String, assessmentID: UUID, deadlineCopy: String) = mapOf(
      "name" to applicantName,
      "crn" to crn,
      "assessmentUrl" to "http://frontend/assessments/$assessmentID",
      "deadlineCopy" to deadlineCopy,
    )
  }

  @Nested
  inner class AssessmentDeallocated {
    @Test
    fun `assessmentDeallocated sends an email when the user has an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(ALLOCATED_EMAIL)
        .produce()
      val assessmentID = UUID.randomUUID()

      service.assessmentDeallocated(applicant, assessmentID, CRN)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentDeallocated,
        mapOf(
          "name" to applicant.name,
          "crn" to CRN,
          "assessmentUrl" to "http://frontend/assessments/$assessmentID",
        ),
      )
    }

    @Test
    fun `assessmentDeallocated does not send an email to a user if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()
      val assessmentID = UUID.randomUUID()

      service.assessmentDeallocated(applicant, assessmentID, CRN)
      mockEmailNotificationService.assertEmailRequestCount(0)
    }
  }

  @Nested
  inner class AppealedAssessmentAllocated {
    @Test
    fun `appealedAssessmentAllocated sends an email when the user has an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(ALLOCATED_EMAIL)
        .produce()
      val assessmentID = UUID.randomUUID()

      service.appealedAssessmentAllocated(applicant, assessmentID, CRN)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.appealedAssessmentAllocated,
        mapOf(
          "name" to applicant.name,
          "crn" to CRN,
          "assessmentUrl" to "http://frontend/assessments/$assessmentID",
        ),
      )
    }

    @Test
    fun `appealedAssessmentAllocated does not send an email to a user if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()
      val assessmentID = UUID.randomUUID()

      service.appealedAssessmentAllocated(applicant, assessmentID, CRN)
      mockEmailNotificationService.assertEmailRequestCount(0)
    }
  }

  @Nested
  inner class AssessmentWithdrawn {

    val withdrawingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withName("mr withdrawer")
      .produce()

    @Test
    fun `assessmentWithdrawn sends an email when the user has an email address and assessment is pending`() {
      val assessment = createAssessment(allocatedUserEmail = ALLOCATED_EMAIL)

      service.assessmentWithdrawn(
        assessment = assessment,
        isAssessmentPending = true,
        withdrawingUser = withdrawingUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentWithdrawn,
        mapOf(
          "crn" to CRN,
          "applicationUrl" to "http://frontend/application/${assessment.application.id}",
        ),
      )
    }

    @Test
    fun `assessmentWithdrawn sends a V2 email when the user has an email address and assessment is pending`() {
      val assessment = createAssessment(allocatedUserEmail = ALLOCATED_EMAIL)

      serviceUsingAps530Improvements.assessmentWithdrawn(
        assessment = assessment,
        isAssessmentPending = true,
        withdrawingUser = withdrawingUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentWithdrawnV2,
        mapOf(
          "crn" to CRN,
          "applicationUrl" to "http://frontend/application/${assessment.application.id}",
          "applicationTimelineUrl" to "http://frontend/application/${assessment.application.id}?tab=timeline",
          "withdrawnBy" to "mr withdrawer",
        ),
      )
    }

    @Test
    fun `assessmentWithdrawn does not send an email to a user if they do not have an email address and assessment is pending`() {
      val assessment = createAssessment(allocatedUserEmail = null)

      service.assessmentWithdrawn(
        assessment = assessment,
        isAssessmentPending = true,
        withdrawingUser = withdrawingUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    @Test
    fun `assessmentWithdrawn does not send an email to a user if they do have an email address but assessment is not pending`() {
      val assessment = createAssessment(allocatedUserEmail = ALLOCATED_EMAIL)

      service.assessmentWithdrawn(
        assessment = assessment,
        isAssessmentPending = false,
        withdrawingUser = withdrawingUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    private fun createAssessment(allocatedUserEmail: String?): ApprovedPremisesAssessmentEntity {
      val allocatedUser: UserEntity = UserEntityFactory()
        .withDefaultProbationRegion()
        .withEmail(allocatedUserEmail)
        .produce()

      val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
        id = UUID.randomUUID(),
        addedAt = OffsetDateTime.now(),
        schema = "{}",
      )

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCrn(CRN)
            .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
            .produce(),
        )
        .withAssessmentSchema(schema)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(null)
        .withIsWithdrawn(false)
        .produce()

      return assessment
    }
  }
}
