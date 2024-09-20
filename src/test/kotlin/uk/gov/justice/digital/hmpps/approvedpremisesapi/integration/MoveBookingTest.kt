package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBedMove
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.util.UUID

class MoveBookingTest : InitialiseDatabasePerClassTestBase() {

  lateinit var premises: PremisesEntity
  lateinit var booking: BookingEntity

  @BeforeEach
  fun setup() {
    premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { `Given a Probation Region`() }
    }

    booking = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(
        bedEntityFactory.produceAndPersist {
          withRoom(
            roomEntityFactory.produceAndPersist {
              withPremises(premises)
            },
          )
        },
      )
    }
  }

  @Test
  fun `Move Bookings without a JWT returns 401 `() {
    webTestClient.post()
      .uri("/premises/${premises.id}/bookings/${booking.id}/moves")
      .bodyValue(
        NewBedMove(
          bedId = UUID.randomUUID(),
          notes = "Some Notes",
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Move Bookings when premises does not exist returns 404`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      webTestClient.post()
        .uri("/premises/${UUID.randomUUID()}/bookings/${booking.id}/moves")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewBedMove(
            bedId = UUID.randomUUID(),
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Move Bookings when booking does not exist returns 404`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${UUID.randomUUID()}/moves")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewBedMove(
            bedId = UUID.randomUUID(),
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Move Bookings when bed does not exist returns 404`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/moves")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewBedMove(
            bedId = UUID.randomUUID(),
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Move Bookings moves a booking to a new bed`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      val newRoom = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val newBed = bedEntityFactory.produceAndPersist {
        withRoom(newRoom)
      }

      val previousBed = booking.bed!!

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/moves")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewBedMove(
            bedId = newBed.id,
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .is2xxSuccessful

      val updatedBookingEntity = bookingRepository.findByIdOrNull(booking.id)!!

      assertThat(updatedBookingEntity.bed!!.id).isEqualTo(newBed.id)

      val createdBedMoveEntity = bedMoveRepository.findByBooking_IdOrNull(booking.id)!!

      assertThat(createdBedMoveEntity.booking.id).isEqualTo(booking.id)
      assertThat(createdBedMoveEntity.previousBed!!.id).isEqualTo(previousBed.id)
      assertThat(createdBedMoveEntity.newBed.id).isEqualTo(newBed.id)
      assertThat(createdBedMoveEntity.notes).isEqualTo("Some Notes")
    }
  }

  @Test
  fun `Move Bookings moves a booking to a new bed when booking does not currently have bed`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      val existingBooking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(null)
      }

      val newRoom = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val newBed = bedEntityFactory.produceAndPersist {
        withRoom(newRoom)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${existingBooking.id}/moves")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewBedMove(
            bedId = newBed.id,
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .is2xxSuccessful

      val updatedBookingEntity = bookingRepository.findByIdOrNull(existingBooking.id)!!

      assertThat(updatedBookingEntity.bed!!.id).isEqualTo(newBed.id)

      val createdBedMoveEntity = bedMoveRepository.findByBooking_IdOrNull(existingBooking.id)!!

      assertThat(createdBedMoveEntity.booking.id).isEqualTo(existingBooking.id)
      assertThat(createdBedMoveEntity.previousBed).isNull()
      assertThat(createdBedMoveEntity.newBed.id).isEqualTo(newBed.id)
      assertThat(createdBedMoveEntity.notes).isEqualTo("Some Notes")
    }
  }
}
