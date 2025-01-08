package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity

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

    val lostBed = createLostBed(premises, beds.first())

    webTestClient.delete()
      .uri("/internal/premises/${premises.id}")
      .exchange()
      .expectStatus()
      .isOk

    val premisesFromDatabase = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
    val roomsFromDatabase = rooms.map { roomRepository.findByIdOrNull(it.id) }
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }
    val lostBedFromDatabase = cas3VoidBedspacesTestRepository.findByIdOrNull(lostBed.id)

    assertThat(premisesFromDatabase).isNull()
    roomsFromDatabase.forEach { assertThat(it).isNull() }
    bedsFromDatabase.forEach { assertThat(it).isNull() }
    assertThat(lostBedFromDatabase).isNull()
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

    val lostBed = createLostBed(premises, beds.first())

    every { realBedRepository.delete(match { it.id == beds.last().id }) } throws RuntimeException("Database Exception")

    webTestClient.delete()
      .uri("/internal/premises/${premises.id}")
      .exchange()
      .expectStatus()
      .is5xxServerError

    val premisesFromDatabase = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
    val roomsFromDatabase = rooms.map { roomRepository.findByIdOrNull(it.id) }
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }
    val lostBedFromDatabase = cas3VoidBedspacesTestRepository.findByIdOrNull(lostBed.id)

    assertThat(premisesFromDatabase).isNotNull
    roomsFromDatabase.forEach { assertThat(it).isNotNull }
    bedsFromDatabase.forEach { assertThat(it).isNotNull }
    assertThat(lostBedFromDatabase).isNotNull
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

    val lostBed = createLostBed(premises, beds.first())

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
    val lostBedFromDatabase = cas3VoidBedspacesTestRepository.findByIdOrNull(lostBed.id)

    assertThat(premisesFromDatabase).isNotNull
    roomsFromDatabase.forEach { assertThat(it).isNotNull }
    bedsFromDatabase.forEach { assertThat(it).isNotNull }
    assertThat(bookingFromDatabase).isNotNull
    assertThat(lostBedFromDatabase).isNotNull
  }

  private fun createPremises() = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withProbationRegion(givenAProbationRegion())

    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }

  private fun createLostBed(premises: PremisesEntity, bed: BedEntity) = cas3VoidBedspacesEntityFactory.produceAndPersist {
    withPremises(premises)
    withBed(bed)
    withYieldedReason {
      cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
    }
  }
}
