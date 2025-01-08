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

class DeleteRoomTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realBedRepository: BedRepository

  @Test
  fun `Deleting a Room successfully deletes all related entities and deletes the Room itself`() {
    val room = createRoom()

    val beds = bedEntityFactory.produceAndPersistMultiple(3) {
      withRoom(room)
    }

    val lostBed = createVoidBedspace(room.premises, beds.first())

    webTestClient.delete()
      .uri("/internal/room/${room.id}")
      .exchange()
      .expectStatus()
      .isOk

    val roomFromDatabase = roomRepository.findByIdOrNull(room.id)
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }
    val lostBedFromDatabase = cas3VoidBedspacesTestRepository.findByIdOrNull(lostBed.id)

    assertThat(roomFromDatabase).isNull()
    bedsFromDatabase.forEach { assertThat(it).isNull() }
    assertThat(lostBedFromDatabase).isNull()
  }

  @Test
  fun `Deleting a Room is transactional - failure to delete last bed results in all previous deletes being rolled back`() {
    val room = createRoom()

    val beds = bedEntityFactory.produceAndPersistMultiple(3) {
      withRoom(room)
    }

    val lostBed = createVoidBedspace(room.premises, beds.first())

    every { realBedRepository.delete(match { it.id == beds.last().id }) } throws RuntimeException("Database Exception")

    webTestClient.delete()
      .uri("/internal/room/${room.id}")
      .exchange()
      .expectStatus()
      .is5xxServerError

    val roomFromDatabase = roomRepository.findByIdOrNull(room.id)
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }
    val lostBedFromDatabase = cas3VoidBedspacesTestRepository.findByIdOrNull(lostBed.id)

    assertThat(roomFromDatabase).isNotNull
    bedsFromDatabase.forEach { assertThat(it).isNotNull }
    assertThat(lostBedFromDatabase).isNotNull
  }

  @Test
  fun `A Room cannot be deleted if a booking exists on any bed`() {
    val room = createRoom()

    val beds = bedEntityFactory.produceAndPersistMultiple(3) {
      withRoom(room)
    }

    val lostBed = createVoidBedspace(room.premises, beds.first())

    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(room.premises)
      withBed(beds.last())
    }

    webTestClient.delete()
      .uri("/internal/room/${room.id}")
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)

    val roomFromDatabase = roomRepository.findByIdOrNull(room.id)
    val bedsFromDatabase = beds.map { bedRepository.findByIdOrNull(it.id) }
    val bookingFromDatabase = bookingRepository.findByIdOrNull(booking.id)
    val lostBedFromDatabase = cas3VoidBedspacesTestRepository.findByIdOrNull(lostBed.id)

    assertThat(roomFromDatabase).isNotNull
    bedsFromDatabase.forEach { assertThat(it).isNotNull }
    assertThat(bookingFromDatabase).isNotNull
    assertThat(lostBedFromDatabase).isNotNull
  }

  private fun createRoom() = roomEntityFactory.produceAndPersist {
    withYieldedPremises {
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(givenAProbationRegion())
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }
    }
  }

  private fun createVoidBedspace(premises: PremisesEntity, bed: BedEntity) = cas3VoidBedspacesEntityFactory.produceAndPersist {
    withPremises(premises)
    withBed(bed)
    withYieldedReason {
      cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
    }
  }
}
