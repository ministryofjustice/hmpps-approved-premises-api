package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.DEFAULT_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.NEXT_WORKING_DAY_EMERGENCY_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.SAME_DAY_EMERGENCY_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService.Companion.STANDARD_DEADLINE_COPY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

object Cas1AssessmentEmailServiceTestConstants {
  const val ALLOCATED_USER_EMAIL = "applicant@test.com"
  const val DEALLOCATED_USER_EMAIL = "deallocated@test.com"
  const val CRN = "CRN123"
}

class Cas1AssessmentEmailServiceTest {
  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()
  private val mockWorkingDayCountService = mockk<WorkingDayCountService>()

  val service = Cas1AssessmentEmailService(
    mockEmailNotificationService,
    notifyConfig,
    assessmentUrlTemplate = UrlTemplate("http://frontend/assessments/#id"),
    workingDayCountService = mockWorkingDayCountService,
  )

  @BeforeEach
  fun beforeEach() {
    mockEmailNotificationService.reset()
  }

  @Nested
  inner class AssessmentAllocated {
    private val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL)
      .produce()
    private val assessmentID = UUID.randomUUID()

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and no deadline`() {
      service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN, null, false)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(applicant.name, Cas1AssessmentEmailServiceTestConstants.CRN, assessmentID, DEFAULT_DEADLINE_COPY),
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and an emergency assessment with a deadline of today`() {
      val deadline = OffsetDateTime.now().minusHours(2)
      service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN, deadline, true)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(applicant.name, Cas1AssessmentEmailServiceTestConstants.CRN, assessmentID, SAME_DAY_EMERGENCY_DEADLINE_COPY),
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and an emergency assessment with a deadline of next working day`() {
      every { mockWorkingDayCountService.getWorkingDaysCount(any(), any()) } returns 2
      val deadline = OffsetDateTime.now().plusDays(2)

      service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN, deadline, true)

      val expectedDeadlineCopy = NEXT_WORKING_DAY_EMERGENCY_DEADLINE_COPY.format(deadline.toUiFormattedHourOfDay(), deadline.toLocalDate().toUiFormat())

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(applicant.name, Cas1AssessmentEmailServiceTestConstants.CRN, assessmentID, expectedDeadlineCopy),
      )
    }

    @Test
    fun `assessmentAllocated sends an email to a user if they have an email address and a standard deadline`() {
      every { mockWorkingDayCountService.getWorkingDaysCount(any(), any()) } returns 10
      val deadline = OffsetDateTime.now().plusDays(10)

      service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN, deadline, false)

      val expectedDeadlineCopy = STANDARD_DEADLINE_COPY.format("10")

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL,
        notifyConfig.templates.assessmentAllocated,
        expectedAssessmentAllocatedPersonalisation(applicant.name, Cas1AssessmentEmailServiceTestConstants.CRN, assessmentID, expectedDeadlineCopy),
      )
    }

    @Test
    fun `assessmentAllocated does not send an email to a user if they do not have an email address`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()
      val assessmentID = UUID.randomUUID()

      service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN, OffsetDateTime.now(), false)
      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    private fun expectedAssessmentAllocatedPersonalisation(applicantName: String, crn: String, assessmentID: UUID, deadlineCopy: String) = mapOf(
      "name" to applicantName,
      "crn" to crn,
      "assessmentUrl" to "http://frontend/assessments/$assessmentID",
      "deadlineCopy" to deadlineCopy,
    )
  }

  @Test
  fun `assessmentDeallocated sends an email when the user has an email address`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(Cas1AssessmentEmailServiceTestConstants.DEALLOCATED_USER_EMAIL)
      .produce()
    val assessmentID = UUID.randomUUID()

    service.assessmentDeallocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN)

    mockEmailNotificationService.assertEmailRequestCount(1)
    mockEmailNotificationService.assertEmailRequested(
      Cas1AssessmentEmailServiceTestConstants.DEALLOCATED_USER_EMAIL,
      notifyConfig.templates.assessmentDeallocated,
      mapOf(
        "name" to applicant.name,
        "crn" to Cas1AssessmentEmailServiceTestConstants.CRN,
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

    service.assessmentDeallocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN)
    mockEmailNotificationService.assertEmailRequestCount(0)
  }
}
