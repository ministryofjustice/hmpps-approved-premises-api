package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1ApplicationStatusServiceTest {
  @MockK
  private lateinit var applicationRepository: ApplicationRepository

  @MockK
  private lateinit var cas1SpaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  private lateinit var bookingRepository: BookingRepository

  @InjectMockKs
  private lateinit var service: Cas1ApplicationStatusService

  @Nested
  inner class BookingMade {
    @Test
    fun `if not linked to application (manual booking), do nothing`() {
      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(null)
        .produce()

      service.bookingMade(booking)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if linked to application set status to PLACEMENT_ALLOCATED`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.bookingMade(booking)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
    }
  }

  @Nested
  inner class SpaceBookingMade {
    @Test
    fun `if linked to application set status to PLACEMENT_ALLOCATED`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.spaceBookingMade(spaceBooking)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
    }
  }

  @Nested
  inner class BookingCancelled {

    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
      .produce()

    @Test
    fun `if not linked to application (manual booking), do nothing`() {
      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(null)
        .produce()

      service.lastBookingCancelled(booking, true)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if cancellation not user requested, do nothing`() {
      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      service.lastBookingCancelled(booking, false)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if there are still live bookings for the application, do nothing`() {
      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withStatus(BookingStatus.CONFIRMED)
        .produce()

      every { applicationRepository.save(any()) } returns application
      every { service.bookingRepository.findAllByApplication(application) } returns listOf(booking)

      service.lastBookingCancelled(booking, true)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if there are no live bookings for the application, change status to AWAITING_PLACEMENT`() {
      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()
      val cancellations = mutableListOf(
        CancellationEntityFactory()
          .withDefaults()
          .withBooking(booking)
          .produce(),
      )
      val bookingWithCancellation = booking.copy(cancellations = cancellations)

      every { applicationRepository.save(any()) } returns application
      every { bookingRepository.findAllByApplication(application) } returns listOf(bookingWithCancellation)

      service.lastBookingCancelled(booking, true)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
    }
  }

  @Nested
  inner class SpaceBookingCancelled {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
      .produce()

    @Test
    fun `if not linked to application, do nothing`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withApplication(null)
        .produce()

      service.spaceBookingCancelled(booking)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if cancellation not triggered by user request operation do nothing`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      every { cas1SpaceBookingRepository.findAllByApplication(application) } returns listOf(booking)

      service.spaceBookingCancelled(
        spaceBooking = booking,
        isUserRequestedWithdrawal = false,
      )

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if there are still live bookings for the application, do nothing`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      every { applicationRepository.save(any()) } returns application
      every { cas1SpaceBookingRepository.findAllByApplication(application) } returns listOf(booking)

      service.spaceBookingCancelled(booking)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if there are no live bookings for the application, change status to AWAITING_PLACEMENT`() {
      val cancelledBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      every { applicationRepository.save(any()) } returns application
      every { cas1SpaceBookingRepository.findAllByApplication(application) } returns listOf(cancelledBooking)

      service.spaceBookingCancelled(cancelledBooking)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
    }
  }
}
