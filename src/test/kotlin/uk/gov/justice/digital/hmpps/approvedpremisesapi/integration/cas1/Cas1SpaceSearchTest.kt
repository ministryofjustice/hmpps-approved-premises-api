package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_ESAP
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_PIPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceSearchTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var transformer: Cas1SpaceSearchResultsTransformer

  @BeforeEach
  fun setup() {
    postCodeDistrictRepository.deleteAll()
    roomRepository.deleteAll()
    cas1PremisesLocalRestrictionRepository.deleteAll()
    approvedPremisesRepository.deleteAll()
  }

  @Test
  fun `Search for Spaces without JWT returns 401`() {
    webTestClient.post()
      .uri("/cas1/spaces/search")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Requires CAS1_SPACE_BOOKING_CREATE permission`() {
    givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { _, jwt ->
      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = UUID.randomUUID(),
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        spaceCharacteristics = null,
      )

      webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Search for Spaces returns only premises supporting space bookings, ignoring archived`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val premises = (0..4).map {
        givenAnApprovedPremises(
          latitude = (it * -0.01) - 0.08,
          longitude = (it * 0.01) + 51.49,
          supportsSpaceBookings = true,
          characteristics = emptyList(),
        )
      }

      val premiseWithCharacteristics = givenAnApprovedPremises(
        latitude = 1.0,
        longitude = 2.0,
        supportsSpaceBookings = true,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("hasWideAccessToCommunalAreas")!!,
          characteristicRepository.findCas1ByPropertyName("hasWideStepFreeAccess")!!,
          characteristicRepository.findCas1ByPropertyName("hasLift")!!,
        ),
      ).also {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristics(
            characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
          )
        }
      }

      cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.now().minusDays(2))
        withApprovedPremisesId(premiseWithCharacteristics.id)
        withDescription("No hate based offences")
        withCreatedByUserId(user.id)
      }

      cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.now().minusDays(1))
        withApprovedPremisesId(premiseWithCharacteristics.id)
        withDescription("No child rso")
        withCreatedByUserId(user.id)
      }

      cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.now())
        withApprovedPremisesId(premiseWithCharacteristics.id)
        withDescription("No offence against sex workers")
        withCreatedByUserId(user.id)
        withArchived(true)
      }

      // premise that doesn't support space bookings
      givenAnApprovedPremises(
        latitude = (-0.01) - 0.08,
        longitude = (0.01) + 51.49,
        supportsSpaceBookings = false,
      )

      // archived
      givenAnApprovedPremises(
        latitude = (-0.01) - 0.08,
        longitude = (0.01) + 51.49,
        supportsSpaceBookings = true,
        status = PropertyStatus.archived,
      )

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        spaceCharacteristics = null,
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(6)

      assertThatResultMatches(results.results[0], premises[0], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[1], premises[1], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[2], premises[2], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[3], premises[3], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[4], premises[4], expectedCharacteristics = emptyList())
      assertThatResultMatches(
        results.results[5],
        premiseWithCharacteristics,
        expectedCharacteristics = listOf(
          Cas1SpaceCharacteristic.hasWideAccessToCommunalAreas,
          Cas1SpaceCharacteristic.hasWideStepFreeAccess,
          Cas1SpaceCharacteristic.hasLift,
          Cas1SpaceCharacteristic.hasEnSuite,
        ),
        localRestrictions = listOf("No child rso", "No hate based offences"),
      )
    }
  }

  @ParameterizedTest
  @EnumSource
  fun `Only returns APs matching associated gender in application`(gender: ApprovedPremisesGender) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = gender == ApprovedPremisesGender.WOMAN)

      val expectedPremises = (0..4).map {
        givenAnApprovedPremises(
          latitude = (-0.01) - 0.08,
          longitude = (0.01) + 51.49,
          supportsSpaceBookings = true,
          gender = gender,
        )
      }

      val unexpectedPremises = (0..4).map {
        givenAnApprovedPremises(
          latitude = (-0.01) - 0.08,
          longitude = (0.01) + 51.49,
          supportsSpaceBookings = true,
          gender = ApprovedPremisesGender.entries.first { it != gender },
        )
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        spaceCharacteristics = null,
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)

      assertThat(results.results.map { it.premises.id }).containsExactlyInAnyOrder(
        expectedPremises[0].id,
        expectedPremises[1].id,
        expectedPremises[2].id,
        expectedPremises[3].id,
        expectedPremises[4].id,
      )
    }
  }

  @Test
  fun `Filtering APs by AP type characteristics returns APs matching the characteristics`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      fun createAp(characteristics: List<CharacteristicEntity>) = givenAnApprovedPremises(
        characteristics = characteristics,
        supportsSpaceBookings = true,
      )

      val pipe = createAp(getCharacteristics(CAS1_PROPERTY_NAME_PREMISES_PIPE))
      val pipeAndEsap = createAp(
        getCharacteristics(
          CAS1_PROPERTY_NAME_PREMISES_PIPE,
          CAS1_PROPERTY_NAME_PREMISES_ESAP,
        ),
      )
      createAp(getCharacteristics(CAS1_PROPERTY_NAME_PREMISES_ESAP))
      createAp(getCharacteristics(CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH))
      createAp(getCharacteristics(CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED))
      createAp(emptyList())

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        spaceCharacteristics = listOf(Cas1SpaceCharacteristic.isPIPE),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.results.map { it.premises.id }).containsExactlyInAnyOrder(
        pipe.id,
        pipeAndEsap.id,
      )
    }
  }

  private fun assertThatResultMatches(
    actual: Cas1SpaceSearchResult,
    expected: ApprovedPremisesEntity,
    expectedApType: ApType = ApType.normal,
    expectedCharacteristics: List<Cas1SpaceCharacteristic>? = null,
    localRestrictions: List<String> = emptyList(),
  ) {
    assertThat(actual.distanceInMiles).isGreaterThan(0f.toBigDecimal())
    assertThat(actual.premises).isNotNull
    val premises = actual.premises
    assertThat(premises.id).isEqualTo(expected.id)
    assertThat(premises.apType).isEqualTo(expectedApType)
    assertThat(premises.name).isEqualTo(expected.name)
    assertThat(premises.fullAddress).isEqualTo(expected.fullAddress)
    assertThat(premises.postcode).isEqualTo(expected.postcode)

    if (expectedCharacteristics != null) {
      assertThat(premises.characteristics).containsExactlyInAnyOrder(*expectedCharacteristics.toTypedArray())
    }
    if (localRestrictions.isNotEmpty()) {
      assertThat(premises.localRestrictions).containsExactly(*localRestrictions.toTypedArray())
    }
  }

  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by characteristic only returns APs with that characteristic`(characteristic: Cas1SpaceCharacteristic) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val expectedPremises = (0..4).map {
        givenAnApprovedPremises(
          latitude = (it * -0.01) - 0.08,
          longitude = (it * 0.01) + 51.49,
          characteristics = listOf(characteristic.asCharacteristicEntity()),
          supportsSpaceBookings = true,
        )
      }

      expectedPremises.forEach {
        roomEntityFactory.produceAndPersistMultiple(amount = 5) {
          withPremises(it)
          withCharacteristicsList(listOf(characteristic.asCharacteristicEntity()))
        }
      }

      val unexpectedPremises = (0..4).map {
        givenAnApprovedPremises(
          latitude = (it * -0.01) - 0.08,
          longitude = (it * 0.01) + 51.49,
          characteristics = listOf(
            Cas1SpaceCharacteristic.entries.first { it != characteristic }.asCharacteristicEntity(),
          ),
          supportsSpaceBookings = true,
        )
      }

      unexpectedPremises.forEach {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristicsList(
            listOf(
              Cas1SpaceCharacteristic.entries.first { it != characteristic }.asCharacteristicEntity(),
            ),
          )
        }
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        spaceCharacteristics = listOf(characteristic),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)

      assertThat(results.results[0].premises.id).isEqualTo(expectedPremises[0].id)
      assertThat(results.results[1].premises.id).isEqualTo(expectedPremises[1].id)
      assertThat(results.results[2].premises.id).isEqualTo(expectedPremises[2].id)
      assertThat(results.results[3].premises.id).isEqualTo(expectedPremises[3].id)
      assertThat(results.results[4].premises.id).isEqualTo(expectedPremises[4].id)
    }
  }

  @Test
  fun `Filtering APs by multiple characteristics only returns APs with all characteristics`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val expectedPremises = (0..4).map {
        givenAnApprovedPremises(
          latitude = (it * -0.01) - 0.08,
          longitude = (it * 0.01) + 51.49,
          supportsSpaceBookings = true,
          characteristics = Cas1SpaceCharacteristic.entries.slice(1..3).map { it.asCharacteristicEntity() },
        )
      }

      expectedPremises.forEach {
        roomEntityFactory.produceAndPersistMultiple(randomInt(1, 10)) {
          withPremises(it)
          withCharacteristicsList(Cas1SpaceCharacteristic.entries.slice(1..3).map { it.asCharacteristicEntity() })
        }
      }

      val unexpectedPremises = (0..3).map {
        givenAnApprovedPremises(
          latitude = (it * -0.01) - 0.08,
          longitude = (it * 0.01) + 51.49,
          supportsSpaceBookings = true,
          characteristics = listOf(
            Cas1SpaceCharacteristic.entries[it].asCharacteristicEntity(),
          ),
        )
      }

      unexpectedPremises.forEach {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristicsList(
            it.characteristics,
          )
        }
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        spaceCharacteristics = Cas1SpaceCharacteristic.entries.slice(1..3),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  private fun ApType.asCharacteristicProperty() = when (this) {
    ApType.normal -> null
    ApType.pipe -> "isPIPE"
    ApType.esap -> "isESAP"
    ApType.rfap -> "isRecoveryFocussed"
    ApType.mhapStJosephs, ApType.mhapElliottHouse -> "isSemiSpecialistMentalHealth"
  }

  private fun ApType.asCharacteristicEntity() = this.asCharacteristicProperty()?.let {
    getCharacteristic(it)
  }

  private fun getCharacteristics(vararg propertyNames: String) = propertyNames.map {
    getCharacteristic(it)!!
  }

  private fun getCharacteristic(propertyName: String) = characteristicRepository.findByPropertyName(propertyName, ServiceName.approvedPremises.value)

  private fun Cas1SpaceCharacteristic.asCharacteristicEntity() = getCharacteristic(this.value)!!
}
