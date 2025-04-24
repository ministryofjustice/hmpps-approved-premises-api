package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAcceptedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejectedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1ChangeRequestDomainEventServiceTest {

  @MockK
  lateinit var cas1DomainEventService: Cas1DomainEventService

  @MockK
  lateinit var apDeliusContextApiClient: ApDeliusContextApiClient

  @InjectMockKs
  lateinit var service: Cas1ChangeRequestDomainEventService

  @Nested
  inner class PlacementAppealAccepted {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.savePlacementAppealAccepted(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withExpectedArrivalDate(LocalDate.of(2021, 1, 1))
        .withExpectedDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val acceptingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withDecisionMadeByUser(acceptingUser)
        .withSpaceBooking(spaceBooking)
        .produce()

      service.placementAppealAccepted(changeRequest)

      val domainEventArgument = slot<Cas1DomainEvent<PlacementAppealAcceptedEnvelope>>()

      verify(exactly = 1) {
        cas1DomainEventService.savePlacementAppealAccepted(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.placementAppealAccepted)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data.eventDetails

      assertThat(data.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.acceptedBy.username).isEqualTo(acceptingUser.deliusUsername)
      assertThat(data.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.departureOn).isEqualTo(LocalDate.of(2021, 12, 1))
    }
  }

  @Nested
  inner class PlacementAppealCreated {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.savePlacementAppealCreated(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withExpectedArrivalDate(LocalDate.of(2021, 1, 1))
        .withExpectedDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory().withSpaceBooking(spaceBooking).produce()
      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.placementAppealCreated(changeRequest, requestingUser)

      val domainEventArgument = slot<Cas1DomainEvent<PlacementAppealCreatedEnvelope>>()

      verify(exactly = 1) {
        cas1DomainEventService.savePlacementAppealCreated(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.placementAppealCreated)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data.eventDetails

      assertThat(data.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.requestedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.departureOn).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.appealReason.id).isEqualTo(changeRequest.requestReason.id)
    }
  }

  @Nested
  inner class PlacementAppealRejected {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.savePlacementAppealRejected(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withExpectedArrivalDate(LocalDate.of(2021, 1, 1))
        .withExpectedDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withSpaceBooking(spaceBooking)
        .withRejectionReason(Cas1ChangeRequestRejectionReasonEntityFactory().produce())
        .produce()

      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.placementAppealRejected(changeRequest, requestingUser)

      val domainEventArgument = slot<Cas1DomainEvent<PlacementAppealRejectedEnvelope>>()

      verify(exactly = 1) {
        cas1DomainEventService.savePlacementAppealRejected(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.placementAppealRejected)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data.eventDetails

      assertThat(data.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.rejectedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.departureOn).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.rejectionReason.id).isEqualTo(changeRequest.rejectionReason!!.id)
    }
  }
}
