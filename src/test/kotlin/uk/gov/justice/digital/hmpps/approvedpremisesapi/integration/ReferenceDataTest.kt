package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer

class ReferenceDataTest : IntegrationTestBase() {
  @Autowired
  lateinit var departureReasonTransformer: DepartureReasonTransformer

  @Autowired
  lateinit var lostBedReasonTransformer: LostBedReasonTransformer

  @Autowired
  lateinit var moveOnCategoryTransformer: MoveOnCategoryTransformer

  @Autowired
  lateinit var destinationProviderTransformer: DestinationProviderTransformer

  @Autowired
  lateinit var cancellationReasonTransformer: CancellationReasonTransformer

  @Autowired
  lateinit var staffMemberTransformer: StaffMemberTransformer

  @Autowired
  lateinit var localAuthorityAreaTransformer: LocalAuthorityAreaTransformer

  @Autowired
  lateinit var characteristicTransformer: CharacteristicTransformer

  @Test
  fun `Get Characteristics returns 200 with correct body`() {
    characteristicRepository.deleteAll()

    val characteristics = characteristicEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      characteristics.map(characteristicTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/characteristics")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Local Authorities returns 200 with correct body`() {
    localAuthorityAreaRepository.deleteAll()

    val localAuthorities = localAuthorityEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      localAuthorities.map(localAuthorityAreaTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/local-authority-areas")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Departure Reasons returns 200 with correct body`() {
    departureReasonRepository.deleteAll()

    val departureReasons = departureReasonEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      departureReasons.map(departureReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/departure-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Move on Categories returns 200 with correct body`() {
    moveOnCategoryRepository.deleteAll()

    val moveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/move-on-categories")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Destination Providers returns 200 with correct body`() {
    destinationProviderRepository.deleteAll()

    val destinationProviders = destinationProviderEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      destinationProviders.map(destinationProviderTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/destination-providers")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Cancellation Reasons returns 200 with correct body`() {
    cancellationReasonRepository.deleteAll()

    val departureReasons = cancellationReasonEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      departureReasons.map(cancellationReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/cancellation-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Lost Bed Reasons returns 200 with correct body`() {
    lostBedReasonRepository.deleteAll()

    val lostBedReasons = lostBedReasonEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      lostBedReasons.map(lostBedReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/lost-bed-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
