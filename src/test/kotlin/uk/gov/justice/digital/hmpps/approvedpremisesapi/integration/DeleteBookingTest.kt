package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository

class DeleteBookingTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realExtensionRepository: ExtensionRepository

  @Test
  fun `Deleting a Booking successfully deletes all related entities and deletes the Booking itself`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(createPremises())
    }

    val arrival = arrivalEntityFactory.produceAndPersist {
      withBooking(booking)
    }

    val departure = departureEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(departureReasonEntityFactory.produceAndPersist())
      withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
      withDestinationProvider(destinationProviderEntityFactory.produceAndPersist())
    }

    val nonArrival = nonArrivalEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(nonArrivalReasonEntityFactory.produceAndPersist())
    }

    val cancellation = cancellationEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    val confirmation = confirmationEntityFactory.produceAndPersist {
      withBooking(booking)
    }

    val extensions = extensionEntityFactory.produceAndPersistMultiple(3) {
      withBooking(booking)
    }

    webTestClient.delete()
      .uri("/internal/booking/${booking.id}")
      .exchange()
      .expectStatus()
      .isOk

    val bookingFromDatabase = bookingRepository.findByIdOrNull(booking.id)
    val arrivalFromDatabase = arrivalRepository.findByIdOrNull(arrival.id)
    val departureFromDatabase = departureRepository.findByIdOrNull(departure.id)
    val nonArrivalFromDatabase = nonArrivalRepository.findByIdOrNull(nonArrival.id)
    val cancellationFromDatabase = cancellationRepository.findByIdOrNull(cancellation.id)
    val confirmationFromDatabase = confirmationRepository.findByIdOrNull(confirmation.id)
    val extensionsFromDatabase = extensions.map { extensionRepository.findByIdOrNull(it.id) }

    assertThat(bookingFromDatabase).isNull()
    assertThat(arrivalFromDatabase).isNull()
    assertThat(departureFromDatabase).isNull()
    assertThat(nonArrivalFromDatabase).isNull()
    assertThat(cancellationFromDatabase).isNull()
    assertThat(confirmationFromDatabase).isNull()
    extensionsFromDatabase.forEach { assertThat(it).isNull() }
  }

  @Test
  fun `Deleting a Booking is transactional - failure to delete last extension results in all previous deletes being rolled back`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(createPremises())
    }

    val arrival = arrivalEntityFactory.produceAndPersist {
      withBooking(booking)
    }

    val departure = departureEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(departureReasonEntityFactory.produceAndPersist())
      withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
      withDestinationProvider(destinationProviderEntityFactory.produceAndPersist())
    }

    val nonArrival = nonArrivalEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(nonArrivalReasonEntityFactory.produceAndPersist())
    }

    val cancellation = cancellationEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    val confirmation = confirmationEntityFactory.produceAndPersist {
      withBooking(booking)
    }

    val extensions = extensionEntityFactory.produceAndPersistMultiple(3) {
      withBooking(booking)
    }

    every { realExtensionRepository.delete(match { it.id == extensions.last().id }) } throws RuntimeException("Database Exception")

    webTestClient.delete()
      .uri("/internal/booking/${booking.id}")
      .exchange()
      .expectStatus()
      .is5xxServerError

    val bookingFromDatabase = bookingRepository.findByIdOrNull(booking.id)
    val arrivalFromDatabase = arrivalRepository.findByIdOrNull(arrival.id)
    val departureFromDatabase = departureRepository.findByIdOrNull(departure.id)
    val nonArrivalFromDatabase = nonArrivalRepository.findByIdOrNull(nonArrival.id)
    val cancellationFromDatabase = cancellationRepository.findByIdOrNull(cancellation.id)
    val confirmationFromDatabase = confirmationRepository.findByIdOrNull(confirmation.id)
    val extensionsFromDatabase = extensions.map { extensionRepository.findByIdOrNull(it.id) }

    assertThat(bookingFromDatabase).isNotNull
    assertThat(arrivalFromDatabase).isNotNull
    assertThat(departureFromDatabase).isNotNull
    assertThat(nonArrivalFromDatabase).isNotNull
    assertThat(cancellationFromDatabase).isNotNull
    assertThat(confirmationFromDatabase).isNotNull
    assertThat(extensionsFromDatabase).isNotNull
  }

  private fun createPremises() = approvedPremisesEntityFactory.produceAndPersist {
    withProbationRegion(
      probationRegionEntityFactory.produceAndPersist {
        withApArea(apAreaEntityFactory.produceAndPersist())
      }
    )

    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }
}
