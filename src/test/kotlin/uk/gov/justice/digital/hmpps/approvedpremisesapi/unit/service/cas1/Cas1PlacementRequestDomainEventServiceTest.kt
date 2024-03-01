package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementRequestDomainEventServiceTest {

  private object TestConstants {
    const val CRN = "CRN123"
  }

  val domainEventService = mockk<DomainEventService>()
  val domainEventTransformer = mockk<DomainEventTransformer>()
  val service = Cas1PlacementRequestDomainEventService(
    domainEventService,
    domainEventTransformer,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
  )

  @Nested
  inner class PlacementRequestWithdrawn {

    @Test
    fun `it creates a domain event if for the applications arrival date`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(placementRequirements)
        .withPlacementApplication(null)
        .withAssessment(assessment)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
        .withExpectedArrival(LocalDate.of(2024, 5, 3))
        .withDuration(7)
        .produce()

      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.saveMatchRequestWithdrawnEvent(any()) } returns Unit

      service.placementRequestWithdrawn(
        placementRequest,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.PlacementApplication,
          placementRequest.id,
        ),
      )

      verify(exactly = 1) {
        domainEventService.saveMatchRequestWithdrawnEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.matchRequestId == placementRequest.id &&
              data.personReference == PersonReference(
              crn = application.crn,
              noms = application.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawnBy == withdrawnBy
            data.withdrawalReason == "DUPLICATE_PLACEMENT_REQUEST" &&
              data.requestIsForApplicationsArrivalDate == true &&
              data.datePeriod ==
              DatePeriod(
                LocalDate.of(2024, 5, 3),
                LocalDate.of(2024, 5, 10),
              )
          },
        )
      }
    }

    @Test
    fun `it doesnt creates a domain event if not for the applications arrival date`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(placementRequirements)
        .withPlacementApplication(placementApplication)
        .withAssessment(assessment)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
        .withExpectedArrival(LocalDate.of(2024, 5, 3))
        .withDuration(7)
        .produce()

      service.placementRequestWithdrawn(
        placementRequest,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.PlacementApplication,
          placementRequest.id,
        ),
      )

      verify { domainEventService wasNot Called }
    }
  }
}
