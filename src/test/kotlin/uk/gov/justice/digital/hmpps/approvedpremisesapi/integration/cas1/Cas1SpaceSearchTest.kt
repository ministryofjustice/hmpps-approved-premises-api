package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer
import java.time.LocalDate

class Cas1SpaceSearchTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var transformer: Cas1SpaceSearchResultsTransformer

  @BeforeEach
  fun setup() {
    postCodeDistrictRepository.deleteAll()
    roomRepository.deleteAll()
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
  fun `Search for Spaces returns OK with correct body`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
      }

      val searchParameters = Cas1SpaceSearchParameters(
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = null,
          genders = null,
        ),
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
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], premises[0])
      assertThatResultMatches(results.results[1], premises[1])
      assertThatResultMatches(results.results[2], premises[2])
      assertThatResultMatches(results.results[3], premises[3])
      assertThatResultMatches(results.results[4], premises[4])
    }
  }

  @Disabled("Only male APs are currently supported in the CAS1 service")
  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by gender only returns APs supporting that gender`(gender: Gender) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { _, jwt ->
      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        // withGender(gender)
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        // withGender(Gender.entries.first { it != gender })
      }

      val searchParameters = Cas1SpaceSearchParameters(
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = null,
          genders = listOf(gender),
        ),
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
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by AP type returns only APs of that type`(apType: ApType) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { _, jwt ->
      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(apType.asCharacteristicEntity()))
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(ApType.entries.first { it != apType }.asCharacteristicEntity()))
      }

      val searchParameters = Cas1SpaceSearchParameters(
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = listOf(apType),
          spaceCharacteristics = null,
          genders = null,
        ),
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
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      val expectedApType = if (apType == ApType.mhapElliottHouse) {
        ApType.mhapStJosephs
      } else {
        apType
      }

      assertThatResultMatches(results.results[0], expectedPremises[0], expectedApType)
      assertThatResultMatches(results.results[1], expectedPremises[1], expectedApType)
      assertThatResultMatches(results.results[2], expectedPremises[2], expectedApType)
      assertThatResultMatches(results.results[3], expectedPremises[3], expectedApType)
      assertThatResultMatches(results.results[4], expectedPremises[4], expectedApType)
    }
  }

  @Test
  fun `Filtering APs by multiple AP types returns APs of any specified type`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { _, jwt ->
      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(4) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(ApType.entries[it - 1].asCharacteristicEntity()))
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(ApType.entries[5].asCharacteristicEntity()))
      }

      val searchParameters = Cas1SpaceSearchParameters(
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries.slice(0..3),
          spaceCharacteristics = null,
          genders = null,
        ),
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

      assertThat(results.resultsCount).isEqualTo(4)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0], ApType.normal)
      assertThatResultMatches(results.results[1], expectedPremises[1], ApType.pipe)
      assertThatResultMatches(results.results[2], expectedPremises[2], ApType.esap)
      assertThatResultMatches(results.results[3], expectedPremises[3], ApType.rfap)
    }
  }

  private fun assertThatResultMatches(
    actual: Cas1SpaceSearchResult,
    expected: ApprovedPremisesEntity,
    expectedApType: ApType = ApType.normal,
  ) {
    assertThat(actual.spacesAvailable).isEmpty()
    assertThat(actual.distanceInMiles).isGreaterThan(0f.toBigDecimal())
    assertThat(actual.premises).isNotNull
    assertThat(actual.premises!!.id).isEqualTo(expected.id)
    assertThat(actual.premises!!.apCode).isEqualTo(expected.apCode)
    assertThat(actual.premises!!.deliusQCode).isEqualTo(expected.qCode)
    assertThat(actual.premises!!.apType).isEqualTo(expectedApType)
    assertThat(actual.premises!!.name).isEqualTo(expected.name)
    assertThat(actual.premises!!.addressLine1).isEqualTo(expected.addressLine1)
    assertThat(actual.premises!!.addressLine2).isEqualTo(expected.addressLine2)
    assertThat(actual.premises!!.town).isEqualTo(expected.town)
    assertThat(actual.premises!!.postcode).isEqualTo(expected.postcode)
    assertThat(actual.premises!!.apArea).isNotNull
    assertThat(actual.premises!!.apArea!!.id).isEqualTo(expected.probationRegion.apArea!!.id)
    assertThat(actual.premises!!.apArea!!.name).isEqualTo(expected.probationRegion.apArea!!.name)
    assertThat(actual.premises!!.totalSpaceCount).isEqualTo(expected.rooms.flatMap { it.beds }.count())
    assertThat(actual.premises!!.premisesCharacteristics).isEmpty()
  }

  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by characteristic only returns APs with that characteristic`(characteristic: Cas1SpaceCharacteristic) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { _, jwt ->
      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOf(characteristic.asCharacteristicEntity()))
      }

      expectedPremises.forEach {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristicsList(listOf(characteristic.asCharacteristicEntity()))
        }
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(
          listOf(
            Cas1SpaceCharacteristic.entries.first { it != characteristic }.asCharacteristicEntity(),
          ),
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
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = listOf(characteristic),
          genders = null,
        ),
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
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  @Test
  fun `Filtering APs by multiple characteristics only returns APs with all characteristics`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { _, jwt ->
      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(Cas1SpaceCharacteristic.entries.slice(1..3).map { it.asCharacteristicEntity() })
      }

      expectedPremises.forEach {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristicsList(Cas1SpaceCharacteristic.entries.slice(1..3).map { it.asCharacteristicEntity() })
        }
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(4) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(
          listOf(
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
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries.slice(1..3),
          genders = null,
        ),
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
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  private fun ApType.asCharacteristicEntity() = when (this) {
    ApType.normal -> null
    ApType.pipe -> "isPIPE"
    ApType.esap -> "isESAP"
    ApType.rfap -> "isRecoveryFocussed"
    ApType.mhapStJosephs, ApType.mhapElliottHouse -> "isSemiSpecialistMentalHealth"
  }?.let {
    characteristicRepository.findByPropertyName(it, ServiceName.approvedPremises.value)
  }

  private fun Cas1SpaceCharacteristic.asCharacteristicEntity() = characteristicRepository.findByPropertyName(this.value, ServiceName.approvedPremises.value)
    ?: characteristicEntityFactory.produceAndPersist {
      withName(this@asCharacteristicEntity.value)
      withPropertyName(this@asCharacteristicEntity.value)
      withServiceScope(ServiceName.approvedPremises.value)
      withModelScope("*")
    }
}
