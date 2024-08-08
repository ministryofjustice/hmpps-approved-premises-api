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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1AssessmentEmailServiceTest.Constants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1AssessmentEmailServiceTest.Constants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentEmailServiceTest {

  object Constants {
    const val APPLICANT_EMAIL = "applicant@test.com"
    const val ALLOCATED_EMAIL = "allocated@test.com"
    const val CRN = "CRN123"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockCas1EmailNotificationService()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  val service = Cas1AssessmentEmailService(
    mockEmailNotificationService,
    notifyConfig,
    assessmentUrlTemplate = UrlTemplate("http://frontend/assessments/#id"),
    applicationUrlTemplate = UrlTemplate("http://frontend/application/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/application/#applicationId?tab=timeline"),
    workingDayService = mockWorkingDayService,
  )

  @BeforeEach
  fun beforeEach() {
    mockEmailNotificationService.reset()
  }

  @Nested
  inner class AssessmentAccepted {
    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    @Test
    fun `assessmentAccepted does not send an email to the application creator if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(CRN)
        .withCreatedByUser(applicant)
        .produce()

      service.assessmentAccepted(application)
      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    @Test
    fun `assessmentAccepted sends an email to the application creator if they have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .withName("The Applicant Name")
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(CRN)
        .withCreatedByUser(applicant)
        .produce()

      service.assessmentAccepted(application)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.assessmentAccepted,
        mapOf(
          "name" to "The Applicant Name",
          "applicationUrl" to "http://frontend/application/${application.id}",
          "crn" to CRN,
        ),
        application,
      )
    }
  }

  @Nested
  inner class AssessmentRejected {
    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    @Test
    fun `assessmentRejected does not send an email to the application creator if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(CRN)
        .withCreatedByUser(applicant)
        .produce()

      service.assessmentRejected(application)
      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    @Test
    fun `assessmentRejected sends an email to the application creator if they have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .withName("The Applicant Name")
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(CRN)
        .withCreatedByUser(applicant)
        .produce()

      service.assessmentRejected(application)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.assessmentRejected,
        mapOf(
          "name" to "The Applicant Name",
          "applicationUrl" to "http://frontend/application/${application.id}",
          "crn" to CRN,
        ),
        application,
      )
    }
  }

  @Nested
  inner class AssessmentAllocated {
    private val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(ALLOCATED_EMAIL)
      .produce()

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withCrn(CRN)
      .produce()

    private val assessmentID = UUID.randomUUID()

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and no deadline`() {
      service.assessmentAllocated(applicant, assessmentID, application, null, false)

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
        application,
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and an emergency assessment with a deadline of today`() {
      val deadline = OffsetDateTime.now()
      service.assessmentAllocated(applicant, assessmentID, application, deadline, true)

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
        application,
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and an emergency assessment with a deadline of next working day`() {
      every { mockWorkingDayService.getCompleteWorkingDaysFromNowUntil(any()) } returns 2
      val deadline = OffsetDateTime.now().plusDays(2)

      service.assessmentAllocated(applicant, assessmentID, application, deadline, true)

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
        application,
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and a standard deadline`() {
      every { mockWorkingDayService.getCompleteWorkingDaysFromNowUntil(any()) } returns 10
      val deadline = OffsetDateTime.now().plusDays(10)

      service.assessmentAllocated(applicant, assessmentID, application, deadline, false)

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
        application,
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
        application,
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

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withCrn(CRN)
        .produce()

      val assessmentID = UUID.randomUUID()

      service.assessmentDeallocated(applicant, assessmentID, application)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.assessmentDeallocated,
        mapOf(
          "name" to applicant.name,
          "crn" to CRN,
          "assessmentUrl" to "http://frontend/assessments/$assessmentID",
        ),
        application,
      )
    }

    @Test
    fun `assessmentDeallocated does not send an email to a user if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withCrn(CRN)
        .produce()

      val assessmentID = UUID.randomUUID()

      service.assessmentDeallocated(applicant, assessmentID, application)
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

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withCrn(CRN)
        .produce()

      val assessmentID = UUID.randomUUID()

      service.appealedAssessmentAllocated(applicant, assessmentID, application)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        ALLOCATED_EMAIL,
        notifyConfig.templates.appealedAssessmentAllocated,
        mapOf(
          "name" to applicant.name,
          "crn" to CRN,
          "assessmentUrl" to "http://frontend/assessments/$assessmentID",
        ),
        application,
      )
    }

    @Test
    fun `appealedAssessmentAllocated does not send an email to a user if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withCrn(CRN)
        .produce()

      val assessmentID = UUID.randomUUID()

      service.appealedAssessmentAllocated(applicant, assessmentID, application)
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
    fun `assessmentWithdrawn sends email when the user has an email address and assessment is pending`() {
      val assessment = createAssessment(allocatedUserEmail = ALLOCATED_EMAIL)
      val application = assessment.application as ApprovedPremisesApplicationEntity

      service.assessmentWithdrawn(
        assessment = assessment,
        application = application,
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
        application,
      )
    }

    @Test
    fun `assessmentWithdrawn does not send an email to a user if they do not have an email address and assessment is pending`() {
      val assessment = createAssessment(allocatedUserEmail = null)
      val application = assessment.application as ApprovedPremisesApplicationEntity

      service.assessmentWithdrawn(
        assessment = assessment,
        application = application,
        isAssessmentPending = true,
        withdrawingUser = withdrawingUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    @Test
    fun `assessmentWithdrawn does not send an email to a user if they do have an email address but assessment is not pending`() {
      val assessment = createAssessment(allocatedUserEmail = ALLOCATED_EMAIL)
      val application = assessment.application as ApprovedPremisesApplicationEntity

      service.assessmentWithdrawn(
        assessment = assessment,
        application = application,
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
