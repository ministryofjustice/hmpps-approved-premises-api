package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class PremisesTest {

  @Nested
  inner class GetPremisesById : IntegrationTestBase() {
    @Autowired
    lateinit var premisesTransformer: PremisesTransformer

    @Test
    fun `Get Premises by ID returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { _, jwt ->
        val premises = (0..5).map {
          givenATemporaryAccommodationPremises()
        }.onEach {
          addRoomsAndBeds(it, roomCount = 4, 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        val premisesToGet = premises[2]
        val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20, 20))

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Premises by ID returns OK with correct body when capacity is used`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { _, jwt ->
        val premises = (0..5).map {
          givenATemporaryAccommodationPremises()
        }.onEach {
          addRoomsAndBeds(it, roomCount = 4, 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises[2])
          withArrivalDate(LocalDate.now().minusDays(2))
          withDepartureDate(LocalDate.now().plusDays(4))
        }

        val premisesToGet = premises[2]
        val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20, 19))

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Premises by ID returns Not Found with correct body`() {
      val idToRequest = UUID.randomUUID().toString()

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/premises/$idToRequest")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectHeader().contentType("application/problem+json")
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("title").isEqualTo("Not Found")
        .jsonPath("status").isEqualTo(404)
        .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
    }

    @Test
    fun `Get Temporary Accommodation Premises by ID for a premises not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val premisesToGet = premises[2]

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `The total bedspaces on a Temporary Accommodation Premises is equal to the sum of the bedspaces in all Rooms attached to the Premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }.also {
          addRoomsAndBeds(it, roomCount = 2, bedsPerRoom = 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        webTestClient.get()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.bedCount").isEqualTo(10)
      }
    }
  }
}

fun IntegrationTestBase.addRoomsAndBeds(premises: PremisesEntity, roomCount: Int, bedsPerRoom: Int, isActive: Boolean = true): List<RoomEntity> = roomEntityFactory.produceAndPersistMultiple(roomCount) {
  withYieldedPremises { premises }
}.onEach {
  bedEntityFactory.produceAndPersistMultiple(bedsPerRoom) {
    withYieldedRoom { it }
    if (!isActive) {
      withEndDate { LocalDate.now().minusDays(Random.nextLong(1, 10)) }
    }
  }
}.map {
  roomTestRepository.getReferenceById(it.id)
}
