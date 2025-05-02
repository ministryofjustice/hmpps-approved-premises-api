package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingManagementEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import java.time.LocalDate

class Cas1BookingManagementEmailServiceTest {

  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1BookingManagementEmailService(mockEmailNotificationService)

  companion object TestConstants {
    const val CRU_MANAGEMENT_AREA_EMAIL = "apAreaEmail@test.com"
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val PLACEMENT_APPLICATION_CREATOR_EMAIL = "placementAppCreatorEmail@test.com"
    const val CRN = "CRN123"
    const val FROM_PREMISES_NAME = "From Premises Name"
    const val TO_PREMISES_NAME = "To Premises Name"
    const val CASE_MANAGER_EMAIL = "caseManager@test.com"
  }

  @Nested
  inner class ArrivalRecorded {

    @Test
    fun `not transfer, do nothing`() {
      service.arrivalRecorded(
        Cas1SpaceBookingEntityFactory()
          .withTransferType(null)
          .produce(),
      )

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `not emergency transfer, do nothing`() {
      service.arrivalRecorded(
        Cas1SpaceBookingEntityFactory()
          .withTransferType(TransferType.PLANNED)
          .produce(),
      )

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `emergency transfer, send email to applicant(s)`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withCrn(CRN)
        .withCreatedByUser(UserEntityFactory().withDefaults().withEmail(APPLICANT_EMAIL).produce())
        .withCaseManagerIsNotApplicant(true)
        .withCaseManagerUserDetails(Cas1ApplicationUserDetailsEntityFactory().withEmailAddress(CASE_MANAGER_EMAIL).produce())
        .withCruManagementArea(Cas1CruManagementAreaEntityFactory().withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL).produce())
        .produce()

      val fromSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withPremises(ApprovedPremisesEntityFactory().withDefaults().withName(FROM_PREMISES_NAME).produce())
        .produce()

      val toSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withPremises(ApprovedPremisesEntityFactory().withDefaults().withName(TO_PREMISES_NAME).produce())
        .withTransferType(TransferType.EMERGENCY)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withApplication(application)
            .withPlacementApplication(
              PlacementApplicationEntityFactory()
                .withDefaults()
                .withApplication(application)
                .withCreatedByUser(UserEntityFactory().withDefaults().withEmail(PLACEMENT_APPLICATION_CREATOR_EMAIL).produce())
                .produce(),
            )
            .produce(),
        )
        .withTransferredFrom(fromSpaceBooking)
        .withCanonicalArrivalDate(LocalDate.of(2025, 1, 2))
        .withCanonicalDepartureDate(LocalDate.of(2025, 2, 1))
        .produce()

      service.arrivalRecorded(toSpaceBooking)

      mockEmailNotificationService.assertEmailRequestCount(4)

      mockEmailNotificationService.assertEmailsRequested(
        setOf(APPLICANT_EMAIL, CASE_MANAGER_EMAIL, PLACEMENT_APPLICATION_CREATOR_EMAIL),
        Cas1NotifyTemplates.TRANSFER_COMPLETE,
        personalisationSubSet = mapOf(
          "crn" to CRN,
          "fromPremisesName" to FROM_PREMISES_NAME,
          "toPremisesName" to TO_PREMISES_NAME,
          "startDate" to "2025-01-02",
          "endDate" to "2025-02-01",
          "lengthStay" to 31,
          "lengthStayUnit" to "days",
        ),
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        Cas1NotifyTemplates.TRANSFER_COMPLETE_EMERGENCY_FOR_CRU,
        personalisationSubSet = mapOf(
          "crn" to CRN,
          "fromPremisesName" to FROM_PREMISES_NAME,
          "toPremisesName" to TO_PREMISES_NAME,
        ),
        application,
      )
    }
  }
}
