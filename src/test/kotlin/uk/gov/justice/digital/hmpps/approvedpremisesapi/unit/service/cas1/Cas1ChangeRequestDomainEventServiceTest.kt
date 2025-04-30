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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEventWithPayload
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

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val acceptingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withDecisionMadeByUser(acceptingUser)
        .withSpaceBooking(spaceBooking)
        .produce()

      service.placementAppealAccepted(changeRequest)

      val domainEventArgument = slot<SaveCas1DomainEventWithPayload<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAccepted>>()

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

      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.acceptedBy.username).isEqualTo(acceptingUser.deliusUsername)
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

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory().withSpaceBooking(spaceBooking).produce()
      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.placementAppealCreated(changeRequest, requestingUser)

      val domainEventArgument = slot<SaveCas1DomainEventWithPayload<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreated>>()

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

      assertThat(data.requestedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.reason.id).isEqualTo(changeRequest.requestReason.id)
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

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2021, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withSpaceBooking(spaceBooking)
        .withRejectionReason(Cas1ChangeRequestRejectionReasonEntityFactory().produce())
        .produce()

      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.placementAppealRejected(changeRequest, requestingUser)

      val domainEventArgument = slot<SaveCas1DomainEventWithPayload<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejected>>()

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

      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.rejectedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.booking.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))
      assertThat(data.reason.id).isEqualTo(changeRequest.rejectionReason!!.id)
    }
  }

  @Nested
  inner class PlannedTransferRequestAccepted {

    @Test
    fun success() {
      every { apDeliusContextApiClient.getStaffDetail("theusername") } returns ClientResult.Success(
        HttpStatus.OK,
        StaffDetailFactory.staffDetail(deliusUsername = "theusername"),
      )

      every { cas1DomainEventService.save(any()) } just Runs

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withExpectedArrivalDate(LocalDate.of(2021, 1, 1))
        .withExpectedDepartureDate(LocalDate.of(2021, 12, 1))
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory().withSpaceBooking(spaceBooking).produce()
      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      val application = ApprovedPremisesApplicationEntityFactory().withDefaults().produce()

      val from = Cas1SpaceBookingEntityFactory()
        .withPremises(ApprovedPremisesEntityFactory().withDefaults().withName("frompremises").produce())
        .withCanonicalArrivalDate(LocalDate.of(2019, 8, 7))
        .withCanonicalDepartureDate(LocalDate.of(2019, 8, 8))
        .withApplication(application).produce()

      val to = Cas1SpaceBookingEntityFactory()
        .withPremises(ApprovedPremisesEntityFactory().withDefaults().withName("topremises").produce())
        .withCanonicalArrivalDate(LocalDate.of(2019, 8, 8))
        .withCanonicalDepartureDate(LocalDate.of(2019, 8, 9))
        .withApplication(application).produce()

      service.plannedTransferRequestAccepted(changeRequest, requestingUser, from, to)

      val domainEvent = getSavedDomainEvent<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestAccepted>()

      assertThat(domainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_ACCEPTED)
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.acceptedBy.username).isEqualTo(requestingUser.deliusUsername)

      assertThat(data.from.bookingId).isEqualTo(from.id)
      assertThat(data.from.premises.name).isEqualTo("frompremises")
      assertThat(data.from.arrivalDate).isEqualTo(LocalDate.of(2019, 8, 7))
      assertThat(data.from.departureDate).isEqualTo(LocalDate.of(2019, 8, 8))

      assertThat(data.to.bookingId).isEqualTo(to.id)
      assertThat(data.to.premises.name).isEqualTo("topremises")
      assertThat(data.to.arrivalDate).isEqualTo(LocalDate.of(2019, 8, 8))
      assertThat(data.to.departureDate).isEqualTo(LocalDate.of(2019, 8, 9))
    }
  }

  @Nested
  inner class PlannedTransferRequestCreated {

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

      val changeRequest = Cas1ChangeRequestEntityFactory().withSpaceBooking(spaceBooking).produce()
      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.plannedTransferRequestCreated(changeRequest, requestingUser)

      val domainEvent = getSavedDomainEvent<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestCreated>()

      assertThat(domainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_CREATED)
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))

      assertThat(data.requestedBy.username).isEqualTo(requestingUser.deliusUsername)
      assertThat(data.reason.id).isEqualTo(changeRequest.requestReason.id)
    }
  }

  @Nested
  inner class PlannedTransferRequestRejected {

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
        .withSpaceBooking(spaceBooking)
        .withRejectionReason(Cas1ChangeRequestRejectionReasonEntityFactory().produce())
        .produce()

      val requestingUser = UserEntityFactory().withDefaults().withDeliusUsername("theusername").produce()

      service.plannedTransferRequestRejected(changeRequest, requestingUser)

      val domainEvent = getSavedDomainEvent<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestRejected>()

      assertThat(domainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_REJECTED)
      assertThat(domainEvent.applicationId).isEqualTo(changeRequest.placementRequest.application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(changeRequest.spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isNull()
      assertThat(domainEvent.crn).isEqualTo(changeRequest.placementRequest.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(changeRequest.placementRequest.application.nomsNumber)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data

      assertThat(data.booking.premises.id).isEqualTo(changeRequest.spaceBooking.premises.id)
      assertThat(data.booking.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.booking.arrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(data.booking.departureDate).isEqualTo(LocalDate.of(2021, 12, 1))

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
