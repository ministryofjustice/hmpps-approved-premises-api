package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLACEMENT_APPEAL_CREATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_REQUESTING_AP
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_TARGET_AP
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_CREATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_REJECTED
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate

class Cas1ChangeRequestEmailServiceTest {

  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1ChangeRequestEmailService(
    mockEmailNotificationService,
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/timeline/#applicationId"),
    cruOpenChangeRequestsUrlTemplate = UrlTemplate("http://frontend/cruDashboard/changeRequests"),
    cruDashboardUrlTemplate = UrlTemplate("http://frontend/cruDashboard"),
    spaceBookingTimelineUrlTemplate = UrlTemplate("http://frontend/premises/#premisesId/spaceBooking/#bookingId/timeline"),
  )

  companion object {
    const val APPLICANT_EMAIL = "applicant@test.com"
    const val CASE_MANAGER_EMAIL = "casemanager@test.com"
    const val CRN = "CRN123"
    const val CRU_MANAGEMENT_AREA_EMAIL = "apAreaEmail@test.com"
    const val PLACEMENT_APPLICATION_CREATOR_EMAIL = "placementapp@test.com"
    const val PREMISES_NAME = "the premises"
    const val PREMISES_EMAIL = "premises@test.com"
    const val OTHER_PREMISES_NAME = "the other premises"
    const val OTHER_PREMISES_EMAIL = "otherpremises@test.com"
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

  val otherPremises = ApprovedPremisesEntityFactory().withDefaults()
    .withEmailAddress(OTHER_PREMISES_EMAIL)
    .withName(OTHER_PREMISES_NAME).produce()

  @Nested
  inner class PlacementAppealCreatedTest {

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

      service.placementAppealCreated(PlacementAppealCreated(changeRequest, UserEntityFactory().withDefaults().produce()))

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
  inner class PlacementAppealAcceptedTest {

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

      service.placementAppealAccepted(PlacementAppealAccepted(changeRequest))

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
  inner class PlacementAppealRejectedTest {

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

      service.placementAppealRejected(PlacementAppealRejected(changeRequest, UserEntityFactory().withDefaults().produce()))

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
    }
  }

  @Nested
  inner class PlannedTransferRequestCreatedTest {

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

      service.plannedTransferRequestCreated(PlannedTransferRequestCreated(changeRequest, UserEntityFactory().withDefaults().produce()))

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        PLANNED_TRANSFER_REQUEST_CREATED,
        mapOf(
          "crn" to CRN,
          "cruOpenChangeRequestsUrl" to "http://frontend/cruDashboard/changeRequests",
          "fromPremisesName" to PREMISES_NAME,
        ),
        application,
      )
    }
  }

  @Nested
  inner class PlannedTransferRequestAcceptedTest {

    private val changeRequest = Cas1ChangeRequestEntityFactory()
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

    private val newSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(otherPremises)
      .withCanonicalArrivalDate(LocalDate.of(2023, 2, 1))
      .withCanonicalDepartureDate(LocalDate.of(2023, 2, 14))
      .produce()

    @Test
    fun `sends email to source and target APs`() {
      service.plannedTransferRequestAccepted(PlannedTransferRequestAccepted(changeRequest, UserEntityFactory().withDefaults().produce(), newSpaceBooking))

      mockEmailNotificationService.assertEmailRequestCount(2)
      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_REQUESTING_AP,
        mapOf(
          "fromPremisesName" to PREMISES_NAME,
          "crn" to CRN,
        ),
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        OTHER_PREMISES_EMAIL,
        PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_TARGET_AP,
        mapOf(
          "fromPremisesName" to PREMISES_NAME,
          "toPremisesName" to OTHER_PREMISES_NAME,
          "crn" to CRN,
          "toPlacementStartDate" to "2023-02-01",
          "toPlacementEndDate" to "2023-02-14",
          "toPlacementLengthStay" to 2,
          "toPlacementLengthStayUnit" to "weeks",
          "toPlacementTimelineUrl" to "http://frontend/premises/${otherPremises.id}/spaceBooking/${newSpaceBooking.id}/timeline",
        ),
        application,
      )
    }
  }

  @Nested
  inner class PlannedTransferRequestRejectedTest {

    @Test
    fun `sends email to AP`() {
      val spaceBooking = Cas1SpaceBookingEntityFactory().withPremises(premises).produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withApplication(application)
            .produce(),
        )
        .withSpaceBooking(spaceBooking)
        .produce()

      service.plannedTransferRequestRejected(PlannedTransferRequestRejected(changeRequest, UserEntityFactory().withDefaults().produce()))

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        PLANNED_TRANSFER_REQUEST_REJECTED,
        mapOf(
          "crn" to CRN,
          "fromPlacementTimelineUrl" to "http://frontend/premises/${premises.id}/spaceBooking/${spaceBooking.id}/timeline",
          "fromPremisesName" to PREMISES_NAME,
        ),
        application,
      )
    }
  }
}
