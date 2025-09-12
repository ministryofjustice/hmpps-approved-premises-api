package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspaceReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ReferralRejectionReasonTransformer
import java.util.UUID

class ReferenceDataTest : IntegrationTestBase() {
  @Autowired
  lateinit var departureReasonTransformer: DepartureReasonTransformer

  @Autowired
  lateinit var cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer

  @Autowired
  lateinit var moveOnCategoryTransformer: MoveOnCategoryTransformer

  @Autowired
  lateinit var destinationProviderTransformer: DestinationProviderTransformer

  @Autowired
  lateinit var cancellationReasonTransformer: CancellationReasonTransformer

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

  @Nested
  inner class GetCharacteristics {

    private fun setupCharacteristics() {
      characteristicRepository.deleteAll()

      Characteristic.ModelScope.entries.forEach { entry ->
        characteristicEntityFactory.produceAndPersist {
          withIsActive(true)
          withModelScope(entry.value)
        }
      }
    }

    private fun doRequest(modelScope: String? = null): MutableList<Characteristic>? {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
      return webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder.path("/reference-data/characteristics")
            .queryParam("modelScope", modelScope)
            .build()
        }
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList(Characteristic::class.java)
        .returnResult()
        .responseBody
    }

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

      val response = doRequest(modelScope = null)

      assertThat(response).isNotNull.hasSize(characteristics.size)
      val expectedResponse = characteristics.map(characteristicTransformer::transformJpaToApi)
      assertThat(response!!.containsAll(expectedResponse)).isTrue()
    }

    @Test
    fun `Get Characteristics returns all model scopes by default`() {
      setupCharacteristics()
      val response = doRequest()
      assertThat(response).hasSize(Characteristic.ModelScope.entries.size)
      assertThat(response!!.map { it.modelScope.value }).containsAll(Characteristic.ModelScope.entries.map { it.value })
    }

    @Test
    fun `Get Characteristics only returns requested characteristics and star model scope `() {
      setupCharacteristics()
      val response = doRequest(modelScope = "PREMISES")
      assertThat(response).isNotNull()
      assertThat(response!!.map { it.modelScope.value }).containsOnly(
        Characteristic.ModelScope.premises.value,
        Characteristic.ModelScope.star.value,
      )
    }

    /*
    this currently returns the same results for BEDSPACE and ROOM, in preparation for the refactoring of CAS3 to use 'bedspace' instead of room and bed.
     */
    @ParameterizedTest
    @ValueSource(strings = ["BEDSPACE", "ROOM"])
    fun `Get Characteristics only returns requested room characteristics and star model scope `(modelScope: String) {
      setupCharacteristics()
      val response = doRequest(modelScope = modelScope)
      assertThat(response).isNotNull()
      assertThat(response!!.map { it.modelScope.value }).containsOnly(
        Characteristic.ModelScope.room.value,
        Characteristic.ModelScope.star.value,
      )
    }
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
    val expectedJson = """
      [{"id":"60d030db-ce2d-4f1a-b24f-42ce15d268f7","name":"Other","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"ae93ec2c-157a-49fd-a4c9-b67f1a9457d8","name":"Bed Withdrawn - Further custodial sentence imposed","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"b5d8a978-eb20-436e-a00f-f1358a3664d5","name":"Admitted to Hospital/Healthcare Facility","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"1de629ea-c5fb-4e91-9b0b-ef7d4d69bd5b","name":"Bed Withdrawn - Serious Incident related to CAS3 placement","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"d6c85a7e-cf9d-4a47-8da2-a25356d34570","name":"Bed Withdrawn – Person on probation no longer in property","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"93153ad1-38a3-4f4f-809e-ba8297c0565a","name":"Bed Withdrawn – Further offence/behaviour/increasing risk","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"94976884-c32f-4162-b3af-214beed0e988","name":"Bed Withdrawn - Recall/Breach","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"b8975a4d-68ff-4f42-9ac8-5136c4b393ef","name":"Deceased","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"166b6b02-6f42-4447-b278-dd9a0b437b54","name":"Planned Move on","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null},
      {"id":"529fe9a0-a644-48db-9e62-83c83a623a93","name":"Person on probation moved to another CAS3 property","serviceScope":"temporary-accommodation","isActive":true,"parentReasonId":null}]
    """.trimIndent()

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
    val expectedJson = """
      [{"id":"33244330-87e9-4cc6-9940-f78586585436","name":"Homeless – rough sleeping","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"a90a77a3-5662-4fa8-85ab-07d0c085052f","name":"Homeless – shelter/emergency hostel/campsite","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"15fb0af2-3406-49c9-81ed-5e42bddf9fc2","name":"Homeless – squat","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"2236cd7e-32c5-4784-a461-8f0aead1d386","name":"Householder (owner – freehold or leasehold)","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"d3b9f02e-c3a5-475b-a5dd-124c058900d9","name":"Long term residential healthcare","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"5dfd0cc4-8be3-4788-a7ba-a84d32efe5ea","name":"Pending (awaiting move on outcome in NDelius – to be updated)","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"0d8dee49-f49a-4e0e-a20e-80e0c18645ce","name":"Support housing","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"b3a5bf51-f5a3-46fb-8de5-c0f1b1ee1e8c","name":"Updated move on outcome - not recorded in NDelius","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"587dc0dc-9073-4992-9d58-5576753050e9","name":"Rental accommodation - private rental","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"12c739fa-7bb1-416d-bbf2-71362578a7f3","name":"Rental accommodation - social rental","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"91158ed5-467e-4ee8-90d9-4f17a5dac82f","name":"Transient/Short term accommodation","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"bd7425b0-ee9a-491c-a64b-c6a034847778","name":"CAS2","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"b6d65622-44ae-42ac-9da0-c6a02532c3d5","name":"Hospital","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"c532986f-462c-4adf-ab2e-583e49f06ec6","name":"Prison (further custodial event)","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"8c392f36-515c-4210-bb72-6255f12abb91","name":"Recalled","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"5d1f4d82-6830-43bf-b197-c5975c0c721b","name":"They’re deceased","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"24e80792-1eee-48fc-9a02-51275c3a6217","name":"Another CAS3 property or bedspace","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"d8e04fa1-9757-4681-bfab-6c61913c8463","name":"CAS1/AP","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"247ae3f4-7958-4c59-97ed-272a39ad411c","name":"Friends or family (settled)","serviceScope":"temporary-accommodation","isActive":true},
      {"id":"3de4665a-c848-4797-ba80-502cacc6f7d7","name":"Friends or family (transient)","serviceScope":"temporary-accommodation","isActive":true}]
    """.trimIndent()

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
    val expectedJson = """
      [{"id":"d2a0d037-53db-4bb2-b9f7-afa07948a3f5","name":"Recording Error (e.g. a data entry error)","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d480","name":"CAS1/AP alternative suitable accommodation provided","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d481","name":"CAS2 alternative accommodation provided","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d482","name":"Changes to booking - new booking not required","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d487","name":"Changes to booking - new booking required (e.g. new release date)","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"3d57413f-ca94-424a-b026-bf9e99ed28fe","name":"Local authority alternative suitable accommodation provided","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d483","name":"No single occupancy bedspace is available","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f023aab7-4bd8-42a3-9f80-7aab3d4f40b8","name":"Other alternative accommodation provided (e.g. friends or family)","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d484","name":"Person on probation failed to arrive (alternative accommodation)","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d489","name":"Person on probation failed to arrive (gate arrest/recall)","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d490","name":"Person on probation failed to arrive (other reason)","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"edd09efb-ef83-42da-a15c-750afd057eb3","name":"Person on probation rejected placement","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d486","name":"Risk or needs cannot be safely managed in CAS3 – new booking not required","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d485","name":"Risk or needs cannot be safely managed in CAS3 – new booking required","isActive":true,"serviceScope":"temporary-accommodation"},
      {"id":"2a7fc443-2f31-4501-90c4-435a6e8e59d3","name":"Supplier unable to accommodate (explain)","isActive":true,"serviceScope":"temporary-accommodation"}]
    """.trimIndent()
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
  fun `Get Void Bedspace Reasons for only Temporary Accommodation returns 200 with correct body`() {
    cas3VoidBedspaceReasonTestRepository.deleteAll()

    val expectedLostBedReasons = cas3VoidBedspaceReasonEntityFactory.produceAndPersistMultiple(10)
    val expectedJson = objectMapper.writeValueAsString(
      expectedLostBedReasons.map(cas3VoidBedspaceReasonTransformer::transformJpaToApi),
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
  fun `Get NonArrival Reasons returns only reasons which are active`() {
    nonArrivalReasonRepository.deleteAll()

    val activeNonArrivalReasons = nonArrivalReasonEntityFactory.produceAndPersistMultiple(10)
    nonArrivalReasonEntityFactory.produceAndPersistMultiple(10) {
      withIsActive(false)
    }
    val expectedJson = objectMapper.writeValueAsString(
      activeNonArrivalReasons.map(nonArrivalReasonTransformer::transformJpaToApi),
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
    val expectedJson = """
      [{"id":"f47ac10b-58cc-4372-a567-0e02b2c3d470","name":"CAS1/AP alternative suitable accommodation provided","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d471","name":"CAS2 alternative accommodation provided","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d472","name":"Consent not given by person on probation","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d473","name":"Local authority alternative suitable accommodation provided (includes Priority Need)","isActive":true},
      {"id":"155ee6dc-ac2a-40d2-a350-90b63fb34a06","name":"No bedspace available in PDU","isActive":true},
      {"id":"21b8569c-ef2e-4059-8676-323098d16aa5","name":"No recourse to public funds (NRPF)","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d474","name":"Not eligible (e.g. already released into the community, HDC)","isActive":true},
      {"id":"88c3b8d5-77c8-4c52-84f0-ec9073e4df50","name":"Not enough time on their licence or post-sentence supervision (PSS)","isActive":true},
      {"id":"a1c7d402-77b5-4335-a67b-eba6a71c70bf","name":"Not enough time to place","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d475","name":"Other alternative accommodation provided (e.g. friends or family)","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d476","name":"Out of region referral","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d477","name":"Person has had maximum CAS3 placements on current sentence","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d478","name":"Previous behavioural concerns in CAS3","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d479","name":"Referral submitted too early","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d480","name":"Remanded in custody or detained","isActive":true},
      {"id":"90e9d919-9a39-45cd-b405-7039b5640668","name":"Risk or needs cannot be safely managed in CAS3","isActive":true},
      {"id":"b19ba749-408f-48c0-907c-11eace2dcf67","name":"Single occupancy bedspace not available","isActive":true},
      {"id":"311de468-078b-4c39-ae42-8d41575b7726","name":"Suitable bedspace not available (not related to single occupancy availability)","isActive":true},
      {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d481","name":"Supplier unable to accommodate (e.g. arson needs cannot be met)","isActive":true}]
    """.trimIndent()

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
