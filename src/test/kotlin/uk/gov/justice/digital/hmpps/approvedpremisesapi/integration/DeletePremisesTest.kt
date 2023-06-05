package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository

class DeletePremisesTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realBedRepository: BedRepository

  @Test
  fun `Deleting a Premises successfully deletes all related entities and deletes the Premises itself`() {
    val premises = createPremises()

    val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
      withPremises(premises)
    }

    val beds = rooms.flatMap {
      bedEntityFactory.produceAndPersistMultiple(3) {
        withRoom(it)
      }
    }

    webTestClient.delete()
      .uri("/internal/premises/${premises.id}")
      .exchange()
      .expectStatus()
      .isOk

    val premisesFromDatabase = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
    val roomsFromDatabase = rooms.map { roomRepository.findByIdOrNull(it.id) }
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }

    assertThat(premisesFromDatabase).isNull()
    roomsFromDatabase.forEach { assertThat(it).isNull() }
    bedsFromDatabase.forEach { assertThat(it).isNull() }
  }

  @Test
  fun `Deleting a Premises is transactional - failure to delete last bed results in all previous deletes being rolled back`() {
    val premises = createPremises()

    val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
      withPremises(premises)
    }

    val beds = rooms.flatMap {
      bedEntityFactory.produceAndPersistMultiple(3) {
        withRoom(it)
      }
    }

    every { realBedRepository.delete(match { it.id == beds.last().id }) } throws RuntimeException("Database Exception")

    webTestClient.delete()
      .uri("/internal/premises/${premises.id}")
      .exchange()
      .expectStatus()
      .is5xxServerError

    val premisesFromDatabase = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
    val roomsFromDatabase = rooms.map { roomRepository.findByIdOrNull(it.id) }
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }

    assertThat(premisesFromDatabase).isNotNull
    roomsFromDatabase.forEach { assertThat(it).isNotNull }
    bedsFromDatabase.forEach { assertThat(it).isNotNull }
  }

  @Test
  fun `A premises cannot be deleted if a booking exists on any bed`() {
    val premises = createPremises()

    val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
      withPremises(premises)
    }

    val beds = rooms.flatMap {
      bedEntityFactory.produceAndPersistMultiple(3) {
        withRoom(it)
      }
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(beds.last())
    }

    webTestClient.delete()
      .uri("/internal/premises/${premises.id}")
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)

    val premisesFromDatabase = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
    val roomsFromDatabase = rooms.map { roomRepository.findByIdOrNull(it.id) }
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }
    val bookingFromDatabase = bookingRepository.findByIdOrNull(booking.id)

    assertThat(premisesFromDatabase).isNotNull
    roomsFromDatabase.forEach { assertThat(it).isNotNull }
    bedsFromDatabase.forEach { assertThat(it).isNotNull }
    assertThat(bookingFromDatabase).isNotNull
  }

  private fun createPremises() = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withProbationRegion(
      probationRegionEntityFactory.produceAndPersist {
        withApArea(apAreaEntityFactory.produceAndPersist())
      },
    )

    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }
}
