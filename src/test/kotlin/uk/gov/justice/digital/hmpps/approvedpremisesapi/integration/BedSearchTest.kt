package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.hamcrest.Matchers
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class BedSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching for a Bed without JWT returns 401`() {
    webTestClient.post()
      .uri("/beds/search")
      .bodyValue(
        ApprovedPremisesBedSearchParameters(
          postcodeDistrict = "AA11",
          maxDistanceMiles = 20,
          requiredCharacteristics = listOf(),
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
            requiredCharacteristics = listOf(),
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
      roles = listOf(UserRole.CAS1_MATCHER),
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
            requiredCharacteristics = listOf(),
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

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
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
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
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
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(),
                ),
              ),
            ),
          ),
        )
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns results that do not include beds with current turnarounds`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
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

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.parse("2022-12-21"))
        withDepartureDate(LocalDate.parse("2023-03-21"))
      }

      val turnaround = turnaroundFactory.produceAndPersist {
        withBooking(booking)
        withWorkingDayCount(2)
      }

      booking.turnarounds = mutableListOf(turnaround)

      GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-03-23"),
            durationDays = 7,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            BedSearchResults(
              resultsRoomCount = 0,
              resultsPremisesCount = 0,
              resultsBedCount = 0,
              results = listOf(),
            ),
          ),
        )
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns results when existing booking departure-date is same as search start-date`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
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

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.parse("2022-12-21"))
        withDepartureDate(LocalDate.parse("2023-03-21"))
      }

      val turnaround = turnaroundFactory.produceAndPersist {
        withBooking(booking)
        withWorkingDayCount(2)
      }

      booking.turnarounds = mutableListOf(turnaround)

      GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-03-21"),
            durationDays = 7,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            BedSearchResults(
              resultsRoomCount = 0,
              resultsPremisesCount = 0,
              resultsBedCount = 0,
              results = listOf(),
            ),
          ),
        )
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns results which include overlapping bookings for rooms in the same premises`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val roomOne = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val roomTwo = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bedOne = bedEntityFactory.produceAndPersist {
        withName("matching bed")
        withRoom(roomOne)
      }

      val bedTwo = bedEntityFactory.produceAndPersist {
        withName("matching bed, but with an overlapping booking")
        withRoom(roomOne)
      }

      val bedThree = bedEntityFactory.produceAndPersist {
        withName("bed in a different room, with an overlapping booking")
        withRoom(roomTwo)
      }

      val overlappingBookingSameRoom = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bedTwo)
        withArrivalDate(LocalDate.parse("2023-07-15"))
        withDepartureDate(LocalDate.parse("2023-08-15"))
        withCrn(randomStringMultiCaseWithNumbers(16))
        withId(UUID.randomUUID())
      }

      val overlappingBookingDifferentRoom = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bedThree)
        withArrivalDate(LocalDate.parse("2023-08-25"))
        withDepartureDate(LocalDate.parse("2023-09-25"))
        withCrn(randomStringMultiCaseWithNumbers(16))
        withId(UUID.randomUUID())
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-08-01"),
            durationDays = 31,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
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
                    bedCount = 3,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomOne.id,
                    name = roomOne.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bedOne.id,
                    name = bedOne.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(
                    TemporaryAccommodationBedSearchResultOverlap(
                      crn = overlappingBookingSameRoom.crn,
                      days = 15,
                      bookingId = overlappingBookingSameRoom.id,
                      roomId = roomOne.id,
                    ),
                    TemporaryAccommodationBedSearchResultOverlap(
                      crn = overlappingBookingDifferentRoom.crn,
                      days = 7,
                      bookingId = overlappingBookingDifferentRoom.id,
                      roomId = roomTwo.id,
                    ),
                  ),
                ),
              ),
            ),
          ),
        )
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns results which include overlapping bookings across multiple premises`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val roomInPremisesOne = roomEntityFactory.produceAndPersist {
        withPremises(premisesOne)
      }

      val roomInPremisesTwo = roomEntityFactory.produceAndPersist {
        withPremises(premisesTwo)
      }

      val matchingBedInPremisesOne = bedEntityFactory.produceAndPersist {
        withName("matching bed in premises one")
        withRoom(roomInPremisesOne)
      }

      val overlappingBedInPremisesOne = bedEntityFactory.produceAndPersist {
        withName("overlapping bed in premises one")
        withRoom(roomInPremisesOne)
      }

      val matchingBedInPremisesTwo = bedEntityFactory.produceAndPersist {
        withName("matching bed in premises two")
        withRoom(roomInPremisesTwo)
      }

      val overlappingBedInPremisesTwo = bedEntityFactory.produceAndPersist {
        withName("overlapping bed in premises two")
        withRoom(roomInPremisesTwo)
      }

      val overlappingBookingForBedInPremisesOne = bookingEntityFactory.produceAndPersist {
        withPremises(premisesOne)
        withBed(overlappingBedInPremisesOne)
        withArrivalDate(LocalDate.parse("2023-07-15"))
        withDepartureDate(LocalDate.parse("2023-08-15"))
        withCrn(randomStringMultiCaseWithNumbers(16))
        withId(UUID.randomUUID())
      }

      val overlappingBookingForBedInPremisesTwo = bookingEntityFactory.produceAndPersist {
        withPremises(premisesTwo)
        withBed(overlappingBedInPremisesTwo)
        withArrivalDate(LocalDate.parse("2023-08-25"))
        withDepartureDate(LocalDate.parse("2023-09-25"))
        withCrn(randomStringMultiCaseWithNumbers(16))
        withId(UUID.randomUUID())
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-08-01"),
            durationDays = 31,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            BedSearchResults(
              resultsRoomCount = 2,
              resultsPremisesCount = 2,
              resultsBedCount = 2,
              results = listOf(
                TemporaryAccommodationBedSearchResult(
                  premises = BedSearchResultPremisesSummary(
                    id = premisesOne.id,
                    name = premisesOne.name,
                    addressLine1 = premisesOne.addressLine1,
                    postcode = premisesOne.postcode,
                    characteristics = listOf(),
                    addressLine2 = premisesOne.addressLine2,
                    town = premisesOne.town,
                    bedCount = 2,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomInPremisesOne.id,
                    name = roomInPremisesOne.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = matchingBedInPremisesOne.id,
                    name = matchingBedInPremisesOne.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(
                    TemporaryAccommodationBedSearchResultOverlap(
                      crn = overlappingBookingForBedInPremisesOne.crn,
                      days = 15,
                      bookingId = overlappingBookingForBedInPremisesOne.id,
                      roomId = roomInPremisesOne.id,
                    ),
                  ),
                ),
                TemporaryAccommodationBedSearchResult(
                  premises = BedSearchResultPremisesSummary(
                    id = premisesTwo.id,
                    name = premisesTwo.name,
                    addressLine1 = premisesTwo.addressLine1,
                    postcode = premisesTwo.postcode,
                    characteristics = listOf(),
                    addressLine2 = premisesTwo.addressLine2,
                    town = premisesTwo.town,
                    bedCount = 2,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomInPremisesTwo.id,
                    name = roomInPremisesTwo.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = matchingBedInPremisesTwo.id,
                    name = matchingBedInPremisesTwo.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(
                    TemporaryAccommodationBedSearchResultOverlap(
                      crn = overlappingBookingForBedInPremisesTwo.crn,
                      days = 7,
                      bookingId = overlappingBookingForBedInPremisesTwo.id,
                      roomId = roomInPremisesTwo.id,
                    ),
                  ),
                ),
              ),
            ),
          ),
        )
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns results which do not include non-overlapping bookings`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val roomOne = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bedOne = bedEntityFactory.produceAndPersist {
        withName("matching bed with no bookings")
        withRoom(roomOne)
      }

      val bedTwo = bedEntityFactory.produceAndPersist {
        withName("matching bed with an non-overlapping booking")
        withRoom(roomOne)
      }

      val nonOverlappingBooking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bedTwo)
        withArrivalDate(LocalDate.parse("2024-01-01"))
        withDepartureDate(LocalDate.parse("2024-01-31"))
        withCrn(randomStringMultiCaseWithNumbers(16))
        withId(UUID.randomUUID())
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-08-01"),
            durationDays = 31,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
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
              resultsBedCount = 2,
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
                    bedCount = 2,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomOne.id,
                    name = roomOne.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bedOne.id,
                    name = bedOne.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(),
                ),
                TemporaryAccommodationBedSearchResult(
                  premises = BedSearchResultPremisesSummary(
                    id = premises.id,
                    name = premises.name,
                    addressLine1 = premises.addressLine1,
                    postcode = premises.postcode,
                    characteristics = listOf(),
                    addressLine2 = premises.addressLine2,
                    town = premises.town,
                    bedCount = 2,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomOne.id,
                    name = roomOne.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bedTwo.id,
                    name = bedTwo.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(),
                ),
              ),
            ),
          ),
        )
        .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
        .jsonPath("$.results[*].overlaps[*].roomId").value(Matchers.not(nonOverlappingBooking.bed?.room!!.id))
    }
  }

  @Test
  fun `Searching for a Temporary Accommodation Bed returns results which do not consider cancelled bookings as overlapping`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    `Given a User`(
      probationRegion = probationRegion,
    ) { _, jwt ->
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val roomOne = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bedOne = bedEntityFactory.produceAndPersist {
        withName("matching bed with no bookings")
        withRoom(roomOne)
      }

      val bedTwo = bedEntityFactory.produceAndPersist {
        withName("matching bed with a cancelled booking")
        withRoom(roomOne)
      }

      val nonOverlappingBooking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bedTwo)
        withArrivalDate(LocalDate.parse("2023-07-15"))
        withDepartureDate(LocalDate.parse("2023-08-15"))
        withCrn(randomStringMultiCaseWithNumbers(16))
        withId(UUID.randomUUID())
      }

      nonOverlappingBooking.cancellations += cancellationEntityFactory.produceAndPersist {
        withBooking(nonOverlappingBooking)
        withYieldedReason {
          cancellationReasonEntityFactory.produceAndPersist()
        }
      }

      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = LocalDate.parse("2023-08-01"),
            durationDays = 31,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = searchPdu.name,
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
              resultsBedCount = 2,
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
                    bedCount = 2,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomOne.id,
                    name = roomOne.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bedOne.id,
                    name = bedOne.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(),
                ),
                TemporaryAccommodationBedSearchResult(
                  premises = BedSearchResultPremisesSummary(
                    id = premises.id,
                    name = premises.name,
                    addressLine1 = premises.addressLine1,
                    postcode = premises.postcode,
                    characteristics = listOf(),
                    addressLine2 = premises.addressLine2,
                    town = premises.town,
                    bedCount = 2,
                  ),
                  room = BedSearchResultRoomSummary(
                    id = roomOne.id,
                    name = roomOne.name,
                    characteristics = listOf(),
                  ),
                  bed = BedSearchResultBedSummary(
                    id = bedTwo.id,
                    name = bedTwo.name,
                  ),
                  serviceName = ServiceName.temporaryAccommodation,
                  overlaps = listOf(),
                ),
              ),
            ),
          ),
        )
        .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
        .jsonPath("$.results[*].overlaps[*].roomId").value(Matchers.not(nonOverlappingBooking.bed?.room!!.id))
    }
  }
}
