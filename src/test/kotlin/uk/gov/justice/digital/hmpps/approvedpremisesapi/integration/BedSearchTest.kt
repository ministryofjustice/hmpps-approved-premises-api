package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.math.BigDecimal
import java.time.LocalDate

class BedSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching for a Bed without JWT returns 401`() {
    webTestClient.post()
      .uri("/beds/search")
      .bodyValue(
        ApprovedPremisesBedSearchParameters(
          postcodeDistrict = "AA11",
          maxDistanceMiles = 20,
          requiredPremisesCharacteristics = listOf(),
          requiredRoomCharacteristics = listOf(),
          startDate = LocalDate.parse("2023-03-23"),
          durationDays = 7,
          serviceName = "approved-premises",
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Searching for an Approved Premises Bed without MATCHER role returns 403`() {
    `Given a User` { _, jwt ->
      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          ApprovedPremisesBedSearchParameters(
            postcodeDistrict = "AA11",
            maxDistanceMiles = 20,
            requiredPremisesCharacteristics = listOf(),
            requiredRoomCharacteristics = listOf(),
            startDate = LocalDate.parse("2023-03-23"),
            durationDays = 7,
            serviceName = "approved-premises",
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Searching for an Approved Premises Bed returns 200 with correct body`() {
    `Given a User`(
      roles = listOf(UserRole.MATCHER),
    ) { _, jwt ->
      val postCodeDistrictLatLong = LatLong(50.1044, -2.3992)
      val tenMilesFromPostcodeDistrict = postCodeDistrictLatLong.plusLatitudeMiles(10)

      val postcodeDistrict = postCodeDistrictFactory.produceAndPersist {
        withOutcode("AA11")
        withLatitude(postCodeDistrictLatLong.latitude)
        withLongitude(postCodeDistrictLatLong.longitude)
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
        withLatitude(tenMilesFromPostcodeDistrict.latitude)
        withLongitude(tenMilesFromPostcodeDistrict.longitude)
        withStatus(PropertyStatus.active)
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName("Matching Bed")
        withRoom(room)
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          ApprovedPremisesBedSearchParameters(
            postcodeDistrict = postcodeDistrict.outcode,
            maxDistanceMiles = 20,
            requiredPremisesCharacteristics = listOf(),
            requiredRoomCharacteristics = listOf(),
            startDate = LocalDate.parse("2023-03-23"),
            durationDays = 7,
            serviceName = "approved-premises",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            BedSearchResults(
              resultsRoomCount = 1,
              resultsPremisesCount = 1,
              resultsBedCount = 1,
              results = listOf(
                ApprovedPremisesBedSearchResult(
                  distanceMiles = BigDecimal("10.016010816899744"),
                  premises = BedSearchResultPremisesSummary(
                    id = premises.id,
                    name = premises.name,
                    addressLine1 = premises.addressLine1,
                    postcode = premises.postcode,
                    characteristics = listOf(),
                    addressLine2 = premises.addressLine2,
                    town = premises.town,
                    bedCount = 1,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = room.id,
                    name = room.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bed.id,
                    name = bed.name,
                  ),
                  serviceName = ServiceName.approvedPremises,
                ),
              ),
            ),
          ),
        )
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns 200 with correct body`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    `Given a User`(
      probationRegion = probationRegion
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withPdu("SEARCH-PDU")
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName("Matching Bed")
        withRoom(room)
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-03-23"),
            durationDays = 7,
            serviceName = "approved-premises",
            probationDeliveryUnit = "SEARCH-PDU",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            BedSearchResults(
              resultsRoomCount = 1,
              resultsPremisesCount = 1,
              resultsBedCount = 1,
              results = listOf(
                TemporaryAccommodationBedSearchResult(
                  premises = BedSearchResultPremisesSummary(
                    id = premises.id,
                    name = premises.name,
                    addressLine1 = premises.addressLine1,
                    postcode = premises.postcode,
                    characteristics = listOf(),
                    addressLine2 = premises.addressLine2,
                    town = premises.town,
                    bedCount = 1,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = room.id,
                    name = room.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bed.id,
                    name = bed.name,
                  ),
                  serviceName = ServiceName.approvedPremises,
                ),
              ),
            ),
          ),
        )
    }
  }
}
