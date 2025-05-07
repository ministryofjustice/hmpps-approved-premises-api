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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEventWithPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestRejected
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
  inner class PlacementAppealAcceptedTest {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val acceptingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLACEMENT_APPEAL)
        .withDecisionMadeByUser(acceptingUser)
        .withSpaceBooking(spaceBooking)
        .produce()

      service.placementAppealAccepted(PlacementAppealAccepted(changeRequest))

      val domainEventArgument = slot<SaveCas1DomainEventWithPayload<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestAccepted>>()

      verify(exactly = 1) {
        cas1DomainEventService.save(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.changeRequestId).isEqualTo(changeRequest.id)
      assertThat(data.changeRequestType).isEqualTo(EventChangeRequestType.PLACEMENT_APPEAL)
      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.id).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.acceptedBy.username).isEqualTo(acceptingUser.deliusUsername)
    }
  }

  @Nested
  inner class PlacementAppealCreatedTest {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLACEMENT_APPEAL)
        .withSpaceBooking(spaceBooking)
        .produce()
      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.placementAppealCreated(PlacementAppealCreated(changeRequest, requestingUser))

      val domainEventArgument = slot<SaveCas1DomainEventWithPayload<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated>>()

      verify(exactly = 1) {
        cas1DomainEventService.save(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.changeRequestId).isEqualTo(changeRequest.id)
      assertThat(data.changeRequestType).isEqualTo(EventChangeRequestType.PLACEMENT_APPEAL)
      assertThat(data.requestedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.id).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.reason.id).isEqualTo(changeRequest.requestReason.id)
    }
  }

  @Nested
  inner class PlacementAppealRejectedTest {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLACEMENT_APPEAL)
        .withSpaceBooking(spaceBooking)
        .withRejectionReason(Cas1ChangeRequestRejectionReasonEntityFactory().produce())
        .produce()

      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.placementAppealRejected(PlacementAppealRejected(changeRequest, requestingUser))

      val domainEventArgument = slot<SaveCas1DomainEventWithPayload<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestRejected>>()

      verify(exactly = 1) {
        cas1DomainEventService.save(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.changeRequestId).isEqualTo(changeRequest.id)
      assertThat(data.changeRequestType).isEqualTo(EventChangeRequestType.PLACEMENT_APPEAL)
      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.rejectedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.booking.id).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.reason.id).isEqualTo(changeRequest.rejectionReason!!.id)
    }
  }

  @Nested
  inner class PlannedTransferRequestAcceptedTest {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(ApprovedPremisesEntityFactory().withDefaults().withName("thepremises").produce())
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val acceptingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLANNED_TRANSFER)
        .withDecisionMadeByUser(acceptingUser)
        .withSpaceBooking(spaceBooking)
        .produce()

      service.plannedTransferRequestAccepted(PlannedTransferRequestAccepted(changeRequest))

      val domainEvent = getSavedDomainEvent<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestAccepted>()

      assertThat(domainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_ACCEPTED)
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.acceptedBy.username).isEqualTo(acceptingUser.deliusUsername)

      assertThat(data.changeRequestId).isEqualTo(changeRequest.id)
      assertThat(data.changeRequestType).isEqualTo(EventChangeRequestType.PLANNED_TRANSFER)

      assertThat(data.booking.id).isEqualTo(spaceBooking.id)
      assertThat(data.booking.premises.name).isEqualTo("thepremises")
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
    }
  }

  @Nested
  inner class PlannedTransferRequestCreatedTest {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLANNED_TRANSFER)
        .withSpaceBooking(spaceBooking)
        .produce()
      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.plannedTransferRequestCreated(PlannedTransferRequestCreated(changeRequest, requestingUser))

      val domainEvent = getSavedDomainEvent<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated>()

      assertThat(domainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_CREATED)
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.changeRequestId).isEqualTo(changeRequest.id)
      assertThat(data.changeRequestType).isEqualTo(EventChangeRequestType.PLANNED_TRANSFER)

      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.id).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))

      assertThat(data.requestedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.reason.id).isEqualTo(changeRequest.requestReason.id)
    }
  }

  @Nested
  inner class PlannedTransferRequestRejectedTest {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLANNED_TRANSFER)
        .withSpaceBooking(spaceBooking)
        .withRejectionReason(Cas1ChangeRequestRejectionReasonEntityFactory().produce())
        .produce()

      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.plannedTransferRequestRejected(PlannedTransferRequestRejected(changeRequest, requestingUser))

      val domainEvent = getSavedDomainEvent<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestRejected>()

      assertThat(domainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED)
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.id).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))

      assertThat(data.changeRequestId).isEqualTo(changeRequest.id)
      assertThat(data.changeRequestType).isEqualTo(EventChangeRequestType.PLANNED_TRANSFER)
      assertThat(data.rejectedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.reason.id).isEqualTo(changeRequest.rejectionReason!!.id)
    }
  }

  private fun <T : Cas1DomainEventPayload> getSavedDomainEvent(): SaveCas1DomainEventWithPayload<T> {
    val domainEventArgument = slot<SaveCas1DomainEventWithPayload<T>>()

    verify(exactly = 1) {
      cas1DomainEventService.save(
        capture(domainEventArgument),
      )
    }

    return domainEventArgument.captured
  }
}
