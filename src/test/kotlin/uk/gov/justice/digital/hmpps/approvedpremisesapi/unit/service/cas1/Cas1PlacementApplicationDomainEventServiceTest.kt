package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementApplicationDomainEventServiceTest {

  private object TestConstants {
    const val CRN = "CRN123"
  }

  val domainEventService = mockk<DomainEventService>()
  val domainEventTransformer = mockk<DomainEventTransformer>()
  val service = Cas1PlacementApplicationDomainEventService(
    domainEventService,
    domainEventTransformer,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
  )

  @Nested
  inner class PlacementApplicationWithdrawn {

    @Test
    fun `it creates a domain event`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2025, 2, 2))
          .withDuration(14)
          .produce(),
      )

      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.savePlacementApplicationWithdrawnEvent(any()) } returns Unit

      service.placementApplicationWithdrawn(
        placementApplication,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
        ),
      )

      verify(exactly = 1) {
        domainEventService.savePlacementApplicationWithdrawnEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.placementApplicationId == placementApplication.id &&
              data.personReference == PersonReference(
              crn = application.crn,
              noms = application.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawnBy == withdrawnBy
            data.withdrawalReason == "ALTERNATIVE_PROVISION_IDENTIFIED" &&
              data.placementDates == listOf(
              DatePeriod(
                LocalDate.of(2024, 5, 3),
                LocalDate.of(2024, 5, 10),
              ),
              DatePeriod(
                LocalDate.of(2025, 2, 2),
                LocalDate.of(2025, 2, 16),
              ),
            )
          },
        )
      }
    }
  }
}
