package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.AREA_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.ASSESSOR_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.CASE_MANAGER_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.CREATOR_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementApplicationEmailServiceTest {
  private object TestConstants {
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val CREATOR_EMAIL = "placmentAppCreator@test.com"
    const val AREA_NAME = "theAreaName"
    const val CRN = "CRN123"
    const val ASSESSOR_EMAIL = "matcherEmail@test.com"
    const val CASE_MANAGER_EMAIL = "caseManager@test.com"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  private val service = Cas1PlacementApplicationEmailService(
    mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
  )

  @Nested
  inner class PlacementApplicationSubmitted {

    @Test
    fun `placementApplicationSubmitted doesnt sent email to placement app creator if no email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      service.placementApplicationSubmitted(placementApplication)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementApplicationSubmitted sends email to placement app creator if email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(CREATOR_EMAIL)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationSubmitted(placementApplication)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
      )

      mockEmailNotificationService.assertEmailRequested(
        CREATOR_EMAIL,
        notifyConfig.templates.placementRequestSubmittedV2,
        personalisation,
      )
    }
  }

  @Nested
  inner class PlacementApplicationAllocated {

    @Test
    fun `placementApplicationAllocated doesnt send email to placement app creator if no email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      service.placementApplicationAllocated(placementApplication)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementApplicationAllocated sends email to placement app creator if email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(CREATOR_EMAIL)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationAllocated(placementApplication)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
      )

      mockEmailNotificationService.assertEmailRequested(
        CREATOR_EMAIL,
        notifyConfig.templates.placementRequestAllocatedV2,
        personalisation,
      )
    }
  }

  @Nested
  inner class PlacementApplicationAccepted {

    @Test
    fun `placementApplicationAccepted doesnt send email to placement app creator if no email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      service.placementApplicationAccepted(placementApplication)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementApplicationAccepted sends email to placement app creator if email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(CREATOR_EMAIL)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationAccepted(placementApplication)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
      )

      mockEmailNotificationService.assertEmailRequested(
        CREATOR_EMAIL,
        notifyConfig.templates.placementRequestDecisionAcceptedV2,
        personalisation,
      )
    }
  }

  @Nested
  inner class PlacementApplicationRejected {

    @Test
    fun `placementApplicationRejected doesnt send email to placement app creator if no email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      service.placementApplicationRejected(placementApplication)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementApplicationRejected sends an email to placement app creator if email address defined`() {
      val creator = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(CREATOR_EMAIL)
        .produce()

      val assessor = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationRejected(placementApplication)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "crn" to TestConstants.CRN,
      )

      mockEmailNotificationService.assertEmailRequested(
        CREATOR_EMAIL,
        notifyConfig.templates.placementRequestDecisionRejectedV2,
        personalisation,
      )
    }
  }

  @Nested
  inner class PlacementApplicationWithdrawn {

    val withdrawnByUser = createUser(
      emailAddress = null,
      name = "withdrawingUser",
    )

    @Test
    fun `placementApplicationWithdrawn doesnt send email to placement app creator or assessor if no email addresses defined`() {
      val applicant = createUser(emailAddress = null)
      val assessor = createUser(emailAddress = null)

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(applicant)
        .withAllocatedToUser(assessor)
        .produce()

      service.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        wasBeingAssessedBy = assessor,
        withdrawingUser = withdrawnByUser,
      )

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementApplicationWithdrawn sends an email to placement app creator if email addresses defined`() {
      val creator = createUser(emailAddress = CREATOR_EMAIL)
      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        wasBeingAssessedBy = null,
        withdrawingUser = withdrawnByUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
        "withdrawnBy" to "withdrawingUser",
      )

      mockEmailNotificationService.assertEmailRequested(
        CREATOR_EMAIL,
        notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation,
      )
    }

    @Test
    fun `placementApplicationWithdrawn sends an email to application creator if email addresses defined`() {
      val creator = createUser(emailAddress = null)
      val applicant = createUser(emailAddress = APPLICANT_EMAIL)
      val application = createApplicationForApplicant(applicant)

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        wasBeingAssessedBy = null,
        withdrawingUser = withdrawnByUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
        "withdrawnBy" to "withdrawingUser",
      )

      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation,
      )
    }

    @Test
    fun `placementApplicationWithdrawn sends an email to placement app creator and case manager if case manager not applicant`() {
      val creator = createUser(emailAddress = CREATOR_EMAIL)
      val application = createApplicationForApplicant(caseManagerNotApplicant = true)

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        wasBeingAssessedBy = null,
        withdrawingUser = withdrawnByUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(2)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
        "withdrawnBy" to "withdrawingUser",
      )

      mockEmailNotificationService.assertEmailRequested(
        CREATOR_EMAIL,
        notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation,
      )
    }

    @Test
    fun `placementApplicationWithdrawn sends an email to assessor if email address defined`() {
      val creator = createUser(emailAddress = null)
      val assessor = createUser(emailAddress = ASSESSOR_EMAIL)

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        wasBeingAssessedBy = assessor,
        withdrawingUser = withdrawnByUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to "2020-03-12",
        "endDate" to "2020-03-22",
        "additionalDatesSet" to "no",
        "withdrawnBy" to "withdrawingUser",
      )

      mockEmailNotificationService.assertEmailRequested(
        ASSESSOR_EMAIL,
        notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation,
      )
    }

    @Test
    fun `placementApplicationWithdrawn sends an email with 'additionalDates' if multiple dates defined due to a legacy placement application`() {
      val creator = createUser(emailAddress = null)
      val assessor = createUser(emailAddress = ASSESSOR_EMAIL)

      val application = createApplicationForApplicant()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(creator)
        .withAllocatedToUser(assessor)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2020, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
        PlacementDateEntityFactory()
          .withExpectedArrival(LocalDate.of(2021, 3, 12))
          .withDuration(10)
          .withPlacementApplication(placementApplication)
          .produce(),
      )

      service.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        wasBeingAssessedBy = assessor,
        withdrawingUser = withdrawnByUser,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "additionalDatesSet" to "yes",
      )

      mockEmailNotificationService.assertEmailRequested(
        ASSESSOR_EMAIL,
        notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation,
      )
    }
  }

  private fun createUser(
    emailAddress: String?,
    name: String = "default name",
  ) = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .withEmail(emailAddress)
    .withName(name)
    .produce()

  private fun createApplicationForApplicant(
    applicant: UserEntity = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce(),
    caseManagerNotApplicant: Boolean = false,
  ) = ApprovedPremisesApplicationEntityFactory()
    .withCrn(TestConstants.CRN)
    .withCreatedByUser(applicant)
    .withSubmittedAt(OffsetDateTime.now())
    .withApArea(ApAreaEntityFactory().withName(AREA_NAME).produce())
    .withCaseManagerIsNotApplicant(caseManagerNotApplicant)
    .withCaseManagerUserDetails(
      if (caseManagerNotApplicant) {
        Cas1ApplicationUserDetailsEntityFactory().withEmailAddress(CASE_MANAGER_EMAIL).produce()
      } else {
        null
      },
    )
    .produce()
}
