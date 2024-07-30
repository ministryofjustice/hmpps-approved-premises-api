package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer
import java.time.LocalDate

class Cas1SpaceSearchTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var transformer: Cas1SpaceSearchResultsTransformer

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

    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea {
              apAreaEntityFactory.produceAndPersist()
            }
          }
        }
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

  private fun assertThatResultMatches(actual: Cas1SpaceSearchResult, expected: ApprovedPremisesEntity) {
    assertThat(actual.spacesAvailable).isEmpty()
    assertThat(actual.distanceInMiles).isGreaterThan(0f.toBigDecimal())
    assertThat(actual.premises).isNotNull
    assertThat(actual.premises!!.id).isEqualTo(expected.id)
    assertThat(actual.premises!!.apCode).isEqualTo(expected.apCode)
    assertThat(actual.premises!!.deliusQCode).isEqualTo(expected.qCode)
    assertThat(actual.premises!!.apType).isEqualTo(ApType.normal)
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
}
