package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ReferralRejectionReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import java.util.UUID

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

  @Autowired
  lateinit var apAreaTransformer: ApAreaTransformer

  @Autowired
  lateinit var nonArrivalReasonTransformer: NonArrivalReasonTransformer

  @Autowired
  lateinit var probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer

  @Autowired
  lateinit var referralRejectionReasonTransformer: ReferralRejectionReasonTransformer

  @Test
  fun `Get Characteristics returns 200 with correct body`() {
    characteristicRepository.deleteAll()

    val characteristics = characteristicEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      characteristics.map(characteristicTransformer::transformJpaToApi),
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
      characteristics.map(characteristicTransformer::transformJpaToApi),
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
      characteristics.map(characteristicTransformer::transformJpaToApi),
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
  fun `Get Characteristics returns only active characteristics by default`() {
    characteristicRepository.deleteAll()

    val characteristics = characteristicEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(true)
    }

    // Unexpected characteristics
    characteristicEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }

    val expectedJson = objectMapper.writeValueAsString(
      characteristics.map(characteristicTransformer::transformJpaToApi),
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
  fun `Get Characteristics returns both active and inactive characteristics when 'includeInactive' query is true`() {
    characteristicRepository.deleteAll()

    val activeCharacteristics = characteristicEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(true)
    }

    val inactiveCharacteristics = characteristicEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }

    val characteristics = activeCharacteristics + inactiveCharacteristics

    val expectedJson = objectMapper.writeValueAsString(
      characteristics.map(characteristicTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/characteristics?includeInactive=true")
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
      localAuthorities.map(localAuthorityAreaTransformer::transformJpaToApi),
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
      departureReasons.map(departureReasonTransformer::transformJpaToApi),
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
      expectedDepartureReasons.map(departureReasonTransformer::transformJpaToApi),
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
      expectedDepartureReasons.map(departureReasonTransformer::transformJpaToApi),
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
  fun `Get Departure Reasons returns only active departure reasons by default`() {
    departureReasonRepository.deleteAll()

    val departureReasons = departureReasonEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(true)
    }

    // Unexpected departure reasons
    departureReasonEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }

    val expectedJson = objectMapper.writeValueAsString(
      departureReasons.map(departureReasonTransformer::transformJpaToApi),
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
  fun `Get Departure Reasons returns both active and inactive departure reasons when 'includeInactive' query is true`() {
    departureReasonRepository.deleteAll()

    val activeDepartureReasons = departureReasonEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(true)
    }

    val inactiveDepartureReasons = departureReasonEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }

    val departureReasons = activeDepartureReasons + inactiveDepartureReasons

    val expectedJson = objectMapper.writeValueAsString(
      departureReasons.map(departureReasonTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/departure-reasons?includeInactive=true")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Move on Categories returns 200 with correct body`() {
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)
    inactivateExistingMoveOnCategories()
    val moveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi),
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
  fun `Get Move on Categories with includeInactive flag set returns active and inactive move on categories`() {
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)
    inactivateExistingMoveOnCategories()
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val result =
      webTestClient
        .get()
        .uri("/reference-data/move-on-categories?includeInactive=true")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectBodyList(MoveOnCategory::class.java)
        .returnResult()
        .responseBody

    var allRecords = moveOnCategoryRepository.findAll()

    assertThat(result!!.size).isEqualTo(allRecords.count())
    assertThat(result.filter { !it.isActive }.count()).isEqualTo(allRecords.count { it.isActive == false })
    assertThat(result.filter { it.isActive }.size).isEqualTo(10)
  }

  @Test
  fun `Get Move on Categories returns categories with asterisk service scope`() {
    inactivateExistingMoveOnCategories()
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope("*")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val result =
      webTestClient
        .get()
        .uri("/reference-data/move-on-categories")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectBodyList(MoveOnCategory::class.java)
        .returnResult()
        .responseBody

    assertThat(result!!.size).isEqualTo(20)
    assertThat(result.filter { it.serviceScope == ServiceName.temporaryAccommodation.value }.size).isEqualTo(10)
    assertThat(result.filter { it.serviceScope == ServiceName.approvedPremises.value }).isEmpty()
    assertThat(result.filter { it.serviceScope == "*" }.size).isEqualTo(10)
  }

  @Test
  fun `Get Move on Categories for only approved premises returns 200 with correct body`() {
    inactivateExistingMoveOnCategories()
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)

    val expectedMoveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedMoveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/move-on-categories")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Move on Categories for only temporary accommodation returns 200 with correct body`() {
    inactivateExistingMoveOnCategories()

    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)

    val expectedMoveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedMoveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/move-on-categories")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Move on Categories returns the same result when not providing includeInactive parameter as when it's set to false `() {
    inactivateExistingMoveOnCategories()
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10)
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    var resultWithIncludeInactiveNull =
      webTestClient
        .get()
        .uri("/reference-data/move-on-categories")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody.contentToString()

    var resultWithIncludeInctiveFalse =
      webTestClient
        .get()
        .uri("/reference-data/move-on-categories?includeInactive=false")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody.contentToString()

    assertEquals(resultWithIncludeInactiveNull, resultWithIncludeInctiveFalse)
  }

  private fun inactivateExistingMoveOnCategories(): List<MoveOnCategoryEntity> {
    val inactive = moveOnCategoryRepository.findAll()
      .map { category -> category.copy(isActive = false) }
    return moveOnCategoryRepository.saveAllAndFlush(inactive)
  }

  @Test
  fun `Get Move on Categories returns only active move on categories by default`() {
    moveOnCategoryRepository.deleteAll()

    val moveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(true)
    }

    // Unexpected move on categories
    moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }

    val expectedJson = objectMapper.writeValueAsString(
      moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi),
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
  fun `Get Move on Categories returns both active and inactive move on categories when 'includeInactive' query is true`() {
    moveOnCategoryRepository.deleteAll()

    val activeMoveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(true)
    }

    val inactiveMoveOnCategories = moveOnCategoryEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }

    val moveOnCategories = activeMoveOnCategories + inactiveMoveOnCategories

    val expectedJson = objectMapper.writeValueAsString(
      moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/move-on-categories?includeInactive=true")
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
      destinationProviders.map(destinationProviderTransformer::transformJpaToApi),
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
      departureReasons.map(cancellationReasonTransformer::transformJpaToApi),
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
    val expectedCancellationReasons = listOf(
      CancellationReason(UUID.fromString("c4ae53be-8bf6-4139-b530-254eb79bf79f"), "An alternative AP placement needs to be found", true, "approved-premises"),
      CancellationReason(UUID.fromString("b43a314a-7e25-459b-8a82-7bbcc9313caa"), "AP has appealed placement", true, "approved-premises"),
      CancellationReason(UUID.fromString("acba3547-ab22-442d-acec-2652e49895f2"), "Booking successfully appealed", false, "approved-premises"),
      CancellationReason(UUID.fromString("eece6c5d-7554-4eaa-884c-d2bdd71ae627"), "Deceased", false, "approved-premises"),
      CancellationReason(UUID.fromString("3a5afbfc-3c0f-11ee-be56-0242ac120002"), "Error in booking", false, "approved-premises"),
      CancellationReason(UUID.fromString("7c310cfd-3952-456d-b0ee-0f7817afe64a"), "Error in Booking Details", false, "approved-premises"),
      CancellationReason(UUID.fromString("6b07a333-f838-4426-8f01-bdf70c965983"), "Person has been deprioritised at this AP", true, "approved-premises"),
      CancellationReason(UUID.fromString("d39572ea-9e42-460c-ae88-b6b30fca0b09"), "Probation Practitioner requested it", true, "approved-premises"),
      CancellationReason(UUID.fromString("bcb90030-b2d3-47d1-b289-a8b8c8898576"), "Related application withdrawn", false, "approved-premises"),
      CancellationReason(UUID.fromString("0a115fa4-6fd0-4b23-8e31-e6d1769c3985"), "Related placement request withdrawn", false, "approved-premises"),
      CancellationReason(UUID.fromString("0e068767-c62e-43b5-866d-f0fb1d02ad83"), "Related request for placement withdrawn", false, "approved-premises"),
      CancellationReason(UUID.fromString("3c2a6820-d59d-4c06-a194-7873e9a7b63a"), "The AP requested it", false, "approved-premises"),
      CancellationReason(UUID.fromString("b5688d29-762d-499c-be42-708729aef5ed"), "The placement is being transferred", false, "approved-premises"),
      CancellationReason(UUID.fromString("a693e972-a092-428f-9222-d802849c7121"), "Withdrawn by Approved Premises", false, "approved-premises"),
      CancellationReason(UUID.fromString("1d6f3c6e-3a86-49b4-bfca-2513a078aba3"), "Other", true, "approved-premises"),
    )

    val expectedJson = objectMapper.writeValueAsString(expectedCancellationReasons)

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
      expectedCancellationReasons.map(cancellationReasonTransformer::transformJpaToApi),
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
      lostBedReasons.map(lostBedReasonTransformer::transformJpaToApi),
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
      expectedLostBedReasons.map(lostBedReasonTransformer::transformJpaToApi),
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
      expectedLostBedReasons.map(lostBedReasonTransformer::transformJpaToApi),
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
    probationAreaProbationRegionMappingRepository.deleteAll()
    probationDeliveryUnitRepository.deleteAll()
    probationRegionRepository.deleteAll()

    val probationRegions = probationRegionEntityFactory.produceAndPersistMultiple(10) {
      withApArea(givenAnApArea())
    }
    val expectedJson = objectMapper.writeValueAsString(
      probationRegions.map(probationRegionTransformer::transformJpaToApi),
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

  @Test
  fun `Get AP Areas returns 200 with correct body`() {
    probationAreaProbationRegionMappingRepository.deleteAll()
    probationDeliveryUnitRepository.deleteAll()
    probationRegionRepository.deleteAll()
    apAreaRepository.deleteAll()

    val apAreas = (1..10).map { givenAnApArea() }
    val expectedJson = objectMapper.writeValueAsString(
      apAreas.map(apAreaTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/ap-areas")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get NonArrival Reasons returns 200 with correct body`() {
    nonArrivalReasonRepository.deleteAll()

    val nonArrivalReasons = nonArrivalReasonEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      nonArrivalReasons.map(nonArrivalReasonTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/non-arrival-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Probation Delivery Units returns 200 with correct body`() {
    probationDeliveryUnitRepository.deleteAll()

    val probationRegions = probationRegionEntityFactory.produceAndPersistMultiple(4) {
      withYieldedApArea { givenAnApArea() }
    }

    val probationDeliveryUnits = mutableListOf<ProbationDeliveryUnitEntity>()
    probationRegions.forEach { probationRegion ->
      probationDeliveryUnits += probationDeliveryUnitFactory.produceAndPersistMultiple(10) {
        withProbationRegion(probationRegion)
      }
    }

    val expectedJson = objectMapper.writeValueAsString(
      probationDeliveryUnits.map(probationDeliveryUnitTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/probation-delivery-units")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Probation Delivery Units by Probation Region returns 200 with correct body`() {
    probationDeliveryUnitRepository.deleteAll()

    val probationRegions = probationRegionEntityFactory.produceAndPersistMultiple(4) {
      withYieldedApArea { givenAnApArea() }
    }

    val probationDeliveryUnits = mutableListOf<ProbationDeliveryUnitEntity>()
    probationRegions.forEach { probationRegion ->
      probationDeliveryUnits += probationDeliveryUnitFactory.produceAndPersistMultiple(10) {
        withProbationRegion(probationRegion)
      }
    }

    val probationRegionId = probationRegions[0].id

    val expectedProbationDeliveryUnits = probationDeliveryUnits.filter { it.probationRegion.id == probationRegionId }

    val expectedJson = objectMapper.writeValueAsString(
      expectedProbationDeliveryUnits.map(probationDeliveryUnitTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/probation-delivery-units?probationRegionId=$probationRegionId")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Referral Rejection Reason returns Forbidden when service name is not provided`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/referral-rejection-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Referral Rejection Reason for only temporary accommodation returns 200 with correct body`() {
    referralRejectionReasonRepository.deleteAll()

    referralRejectionReasonEntityFactory.produceAndPersistMultiple(10)

    val expectedReferralRejectionReasons = referralRejectionReasonEntityFactory.produceAndPersistMultiple(10) {
      withServiceScope(ServiceName.temporaryAccommodation.value)
    }

    val expectedJson = objectMapper.writeValueAsString(
      expectedReferralRejectionReasons.map(referralRejectionReasonTransformer::transformJpaToApi),
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reference-data/referral-rejection-reasons")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
