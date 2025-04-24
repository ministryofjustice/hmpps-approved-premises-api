package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLACEMENT_APPEAL_CREATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

class Cas1ChangeRequestEmailServiceTest {

  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1ChangeRequestEmailService(
    mockEmailNotificationService,
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/timeline/#applicationId"),
    cruDashboardUrlTemplate = UrlTemplate("http://frontend/cruDashboard"),
  )

  companion object {
    const val APPLICANT_EMAIL = "applicant@test.com"
    const val CASE_MANAGER_EMAIL = "casemanager@test.com"
    const val CRN = "CRN123"
    const val CRU_MANAGEMENT_AREA_EMAIL = "apAreaEmail@test.com"
    const val PLACEMENT_APPLICATION_CREATOR_EMAIL = "placementapp@test.com"
    const val PREMISES_NAME = "the premises"
    const val PREMISES_EMAIL = "premises@test.com"
  }

  val application = ApprovedPremisesApplicationEntityFactory()
    .withDefaults()
    .withCrn(CRN)
    .withCruManagementArea(
      Cas1CruManagementAreaEntityFactory()
        .withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL)
        .produce(),
    )
    .withCreatedByUser(
      UserEntityFactory()
        .withDefaults()
        .withEmail(APPLICANT_EMAIL)
        .produce(),
    )
    .withCaseManagerIsNotApplicant(true)
    .withCaseManagerUserDetails(Cas1ApplicationUserDetailsEntityFactory().withEmailAddress(CASE_MANAGER_EMAIL).produce())
    .produce()

  val premises = ApprovedPremisesEntityFactory().withDefaults()
    .withEmailAddress(PREMISES_EMAIL)
    .withName(PREMISES_NAME).produce()

  @Nested
  inner class PlacementAppealCreated {

    @Test
    fun `sends emails to CRU`() {
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withApplication(application)
            .produce(),
        )
        .withSpaceBooking(
          Cas1SpaceBookingEntityFactory()
            .withPremises(premises)
            .produce(),
        )
        .produce()

      service.placementAppealCreated(changeRequest)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        PLACEMENT_APPEAL_CREATED,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
          "cruDashboardUrl" to "http://frontend/cruDashboard",
        ),
        application,
      )
    }
  }

  @Nested
  inner class PlacementAppealAccepted {

    @Test
    fun `sends email to premises and applicants`() {
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withApplication(application)
            .withPlacementApplication(
              PlacementApplicationEntityFactory()
                .withApplication(application)
                .withCreatedByUser(
                  UserEntityFactory().withDefaults().withEmail(PLACEMENT_APPLICATION_CREATOR_EMAIL).produce(),
                )
                .produce(),
            )
            .produce(),
        )
        .withSpaceBooking(
          Cas1SpaceBookingEntityFactory()
            .withPremises(premises)
            .produce(),
        )
        .produce()

      service.placementAppealAccepted(changeRequest)

      mockEmailNotificationService.assertEmailRequestCount(4)

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_PREMISES,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
        ),
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_APPLICANT,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
        ),
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PLACEMENT_APPLICATION_CREATOR_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_APPLICANT,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
        ),
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_APPLICANT,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
        ),
        application,
      )
    }
  }

  @Nested
  inner class PlacementAppealRejected {

    @Test
    fun `sends emails to CRU and Premises`() {
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withApplication(application)
            .produce(),
        )
        .withSpaceBooking(
          Cas1SpaceBookingEntityFactory()
            .withPremises(premises)
            .produce(),
        )
        .produce()

      service.placementAppealRejected(changeRequest)

      mockEmailNotificationService.assertEmailRequestCount(2)

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_APPEAL_REJECTED,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
          "applicationTimelineUrl" to "http://frontend/timeline/${application.id}",
        ),
        application,
      )
      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_APPEAL_REJECTED,
        mapOf(
          "apName" to PREMISES_NAME,
          "crn" to CRN,
          "applicationTimelineUrl" to "http://frontend/timeline/${application.id}",
        ),
        application,
      )
    }
  }
}
