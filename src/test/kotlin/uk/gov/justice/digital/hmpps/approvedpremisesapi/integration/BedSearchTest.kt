package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.math.BigDecimal
import java.time.LocalDate

class BedSearchTest : IntegrationTestBase() {
  @Test
  fun `Search for Beds without JWT returns 401`() {
    webTestClient.post()
      .uri("/beds/search")
      .bodyValue(
        BedSearchParameters(
          postcodeDistrict = "AA11",
          requiredPremisesCharacteristics = emptyList(),
          requiredBedCharacteristics = emptyList(),
          startDate = LocalDate.parse("2023-03-14"),
          durationDays = 7,
          maxDistanceMiles = 10
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Search for Beds without MATCHER role returns 403`() {
    `Given a User` { user, jwt ->
      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .bodyValue(
          BedSearchParameters(
            postcodeDistrict = "AA11",
            requiredPremisesCharacteristics = emptyList(),
            requiredBedCharacteristics = emptyList(),
            startDate = LocalDate.parse("2023-03-14"),
            durationDays = 7,
            maxDistanceMiles = 10
          )
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Search for Beds returns 200`() {
    `Given a User`(
      roles = listOf(UserRole.MATCHER)
    ) { user, jwt ->
      val postcodeDistrict = postCodeDistrictFactory.produceAndPersist {
        withOutcode("AA11")
        withLatitude(50.1044)
        withLongitude(-2.3992)
      }

      val probationRegion = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist()
        }
      }

      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withLatitude(50.1044)
        withLongitude(-2.3992)
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .bodyValue(
          BedSearchParameters(
            postcodeDistrict = "AA11",
            requiredPremisesCharacteristics = emptyList(),
            requiredBedCharacteristics = emptyList(),
            startDate = LocalDate.parse("2023-03-14"),
            durationDays = 7,
            maxDistanceMiles = 10
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            listOf(
              BedSearchResult(
                premisesId = premises.id,
                premisesName = premises.name,
                premisesCharacteristicPropertyNames = listOf(),
                bedId = bed.id,
                bedName = bed.name,
                roomCharacteristicPropertyNames = listOf(),
                distanceMiles = BigDecimal.ZERO
              )
            )
          )
        )
    }
  }
}
