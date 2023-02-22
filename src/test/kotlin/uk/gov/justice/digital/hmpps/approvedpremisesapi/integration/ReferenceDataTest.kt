package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
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

  @Autowired
  lateinit var probationRegionTransformer: ProbationRegionTransformer

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
  fun `Get Characteristics for only temporary accommodation returns 200 with correct body`() {
    characteristicRepository.deleteAll()

    val characteristics = characteristicEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }
    val expectedJson = objectMapper.writeValueAsString(
      characteristics.map(characteristicTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/characteristics")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Characteristics for only approved premises returns 200 with correct body`() {
    characteristicRepository.deleteAll()

    val characteristics = characteristicEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }
    val expectedJson = objectMapper.writeValueAsString(
      characteristics.map(characteristicTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/characteristics")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "approved-premises")
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
  fun `Get Departure Reasons for only approved premises returns 200 with correct body`() {
    departureReasonRepository.deleteAll()

    departureReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedDepartureReasons = departureReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedDepartureReasons.map(departureReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/departure-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "approved-premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Departure Reasons for only temporary accommodation returns 200 with correct body`() {
    departureReasonRepository.deleteAll()

    departureReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedDepartureReasons = departureReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedDepartureReasons.map(departureReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/departure-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
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
  fun `Get Move on Categories for only approved premises returns 200 with correct body`() {
    moveOnCategoryRepository.deleteAll()

    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)

    val expectedMoveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedMoveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/move-on-categories")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "approved-premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Move on Categories for only temporary accommodation returns 200 with correct body`() {
    moveOnCategoryRepository.deleteAll()

    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)

    val expectedMoveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedMoveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/move-on-categories")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
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
  fun `Get Cancellation Reasons for only approved premises returns 200 with correct body`() {
    cancellationReasonRepository.deleteAll()

    cancellationReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedCancellationReasons = cancellationReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedCancellationReasons.map(cancellationReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/cancellation-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "approved-premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Cancellation Reasons for only temporary accommodation returns 200 with correct body`() {
    cancellationReasonRepository.deleteAll()

    cancellationReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedCancellationReasons = cancellationReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedCancellationReasons.map(cancellationReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/cancellation-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
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

  @Test
  fun `Get Lost Bed Reasons for only Approved Premises returns 200 with correct body`() {
    lostBedReasonRepository.deleteAll()

    lostBedReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedLostBedReasons = lostBedReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }
    val expectedJson = objectMapper.writeValueAsString(
      expectedLostBedReasons.map(lostBedReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/lost-bed-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "approved-premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Lost Bed Reasons for only Temporary Accommodation returns 200 with correct body`() {
    lostBedReasonRepository.deleteAll()

    lostBedReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedLostBedReasons = lostBedReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }
    val expectedJson = objectMapper.writeValueAsString(
      expectedLostBedReasons.map(lostBedReasonTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/lost-bed-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Probation Regions returns 200 with correct body`() {
    probationRegionRepository.deleteAll()

    val probationRegions = probationRegionEntityFactory.produceAndPersistMultiple(10) {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }
    val expectedJson = objectMapper.writeValueAsString(
      probationRegions.map(probationRegionTransformer::transformJpaToApi)
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/probation-regions")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
