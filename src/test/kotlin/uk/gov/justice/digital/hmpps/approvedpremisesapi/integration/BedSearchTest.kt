package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
@SuppressWarnings("LargeClass")
class BedSearchTest : IntegrationTestBase() {

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = `Given a Probation Region`()
  }

  @Nested
  inner class BedSearchForApprovedPremises {
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
  }

  @Nested
  inner class BedSearchForTemporaryAccommodationPremises {
    @Test
    fun `Searching for a Temporary Accommodation Bed returns 200 with correct body`() {
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
          withNotes(randomStringMultiCaseWithNumbers(100))
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
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    room,
                    bed,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
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
          withServiceName(ServiceName.temporaryAccommodation)
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

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(
          jwt,
          LocalDate.parse("2023-03-23"),
          7,
          searchPdu.name,
        )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results when existing booking departure-date is same as search start-date`() {
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
          withServiceName(ServiceName.temporaryAccommodation)
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

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(
          jwt,
          LocalDate.parse("2023-03-21"),
          7,
          searchPdu.name,
        )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which include overlapping bookings for rooms in the same premises`() {
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
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBed(bedTwo)
          withArrivalDate(LocalDate.parse("2023-07-15"))
          withDepartureDate(LocalDate.parse("2023-08-15"))
          withCrn(randomStringMultiCaseWithNumbers(16))
          withId(UUID.randomUUID())
        }

        val overlappingBookingDifferentRoom = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
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
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 3,
                    numberOfBookedBeds = 2,
                    premisesCharacteristics = listOf(),
                    listOf(
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
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
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
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premisesOne)
          withBed(overlappingBedInPremisesOne)
          withArrivalDate(LocalDate.parse("2023-07-15"))
          withDepartureDate(LocalDate.parse("2023-08-15"))
          withCrn(randomStringMultiCaseWithNumbers(16))
          withId(UUID.randomUUID())
        }

        val overlappingBookingForBedInPremisesTwo = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
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
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomInPremisesOne,
                    matchingBedInPremisesOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 1,
                    premisesCharacteristics = listOf(),
                    listOf(
                      TemporaryAccommodationBedSearchResultOverlap(
                        crn = overlappingBookingForBedInPremisesOne.crn,
                        days = 15,
                        bookingId = overlappingBookingForBedInPremisesOne.id,
                        roomId = roomInPremisesOne.id,
                      ),
                    ),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesTwo,
                    roomInPremisesTwo,
                    matchingBedInPremisesTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 1,
                    premisesCharacteristics = listOf(),
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
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which do not include non-overlapping bookings`() {
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
          withServiceName(ServiceName.temporaryAccommodation)
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
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
          .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
          .jsonPath("$.results[*].overlaps[*].roomId").value(Matchers.not(nonOverlappingBooking.bed?.room!!.id))
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which do not consider cancelled bookings as overlapping`() {
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
          withServiceName(ServiceName.temporaryAccommodation)
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
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
          .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
          .jsonPath("$.results[*].overlaps[*].roomId").value(Matchers.not(nonOverlappingBooking.bed?.room!!.id))
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bedspace in a Shared Property returns only bedspaces in shared properties`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val beds = createPremisesAndBedsWithCharacteristics(
          localAuthorityArea,
          searchPdu,
          BedSearchAttributes.sharedProperty,
        )

        val expextedPremisesOneBedOne = beds.first()
        val expextedPremisesOneRoomOne = expextedPremisesOneBedOne.room
        val expextedPremisesTwoBedOne = beds.drop(1).first()
        val expextedPremisesTwoRoomOne = expextedPremisesTwoBedOne.room
        val expextedPremisesThreeBedOne = beds.drop(2).first()
        val expextedPremisesThreeRoomOne = expextedPremisesThreeBedOne.room

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2024-08-27"),
              durationDays = 84,
              serviceName = "temporary-accommodation",
              probationDeliveryUnit = searchPdu.name,
              attributes = listOf(BedSearchAttributes.sharedProperty),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 3,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesOneRoomOne.premises,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Shared property",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesTwoRoomOne.premises,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Men only",
                      ),
                      CharacteristicPair(
                        propertyName = null,
                        name = "Shared property",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesThreeRoomOne.premises,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Shared property",
                      ),
                      CharacteristicPair(
                        propertyName = null,
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bedspace in a Single Occupancy Property returns only bedspaces in properties with single occupancy`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val beds = createPremisesAndBedsWithCharacteristics(
          localAuthorityArea,
          searchPdu,
          BedSearchAttributes.singleOccupancy,
        )

        val expextedPremisesOneBedOne = beds.first()
        val expextedPremisesOneRoomOne = expextedPremisesOneBedOne.room
        val expextedPremisesTwoBedOne = beds.drop(1).first()
        val expextedPremisesTwoRoomOne = expextedPremisesTwoBedOne.room
        val expextedPremisesThreeBedOne = beds.drop(2).first()
        val expextedPremisesThreeRoomOne = expextedPremisesThreeBedOne.room

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2024-08-27"),
              durationDays = 84,
              serviceName = "temporary-accommodation",
              probationDeliveryUnit = searchPdu.name,
              attributes = listOf(BedSearchAttributes.singleOccupancy),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 3,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesOneRoomOne.premises,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Single occupancy",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesThreeRoomOne.premises,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Single occupancy",
                      ),
                      CharacteristicPair(
                        propertyName = null,
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesTwoRoomOne.premises,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Women only",
                      ),
                      CharacteristicPair(
                        propertyName = null,
                        name = "Single occupancy",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bedspace with wheelchair accessible returns only bedspaces in properties with wheelchair accessible`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val beds = createPremisesAndBedsWithCharacteristics(
          localAuthorityArea,
          searchPdu,
          BedSearchAttributes.wheelchairAccessible,
        )

        val expextedPremisesOneBedOne = beds.first()
        val expextedPremisesOneRoomOne = expextedPremisesOneBedOne.room
        val expextedPremisesTwoBedOne = beds.drop(1).first()
        val expextedPremisesTwoRoomOne = expextedPremisesTwoBedOne.room
        val expextedPremisesThreeBedOne = beds.drop(2).first()
        val expextedPremisesThreeRoomOne = expextedPremisesThreeBedOne.room

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2024-08-27"),
              durationDays = 84,
              serviceName = "temporary-accommodation",
              probationDeliveryUnit = searchPdu.name,
              attributes = listOf(BedSearchAttributes.wheelchairAccessible),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 3,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesOneRoomOne.premises,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Shared property",
                      ),
                      CharacteristicPair(
                        propertyName = null,
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesTwoRoomOne.premises,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Single occupancy",
                      ),
                      CharacteristicPair(
                        propertyName = null,
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesThreeRoomOne.premises,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = null,
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate is same as search start date`() {
      `Given a User` { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, searchStartDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.name)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate is between search start date and end date`() {
      `Given a User` { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(2)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.name)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate is same as search end date`() {
      `Given a User` { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(durationDays.toLong() - 1)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.name)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate less than than search start date`() {
      `Given a User` { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.minusDays(1)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.name)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return single bed when given premises has got 2 rooms where one with endDate and another room without enddate`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.parse("2023-03-23")
        val durationDays = 7
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

        bedEntityFactory.produceAndPersist {
          withName("not Matching Bed")
          withEndDate { searchStartDate.plusDays(2) }
          withRoom(room)
        }

        val roomWithoutEndDate = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bedWithoutEndDate = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withRoom(roomWithoutEndDate)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
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
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomWithoutEndDate,
                    bedWithoutEndDate,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return bed when given premises bedspace endDate after search end date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
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
          withEndDate { searchStartDate.plusDays(durationDays.toLong() + 2) }
          withRoom(room)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
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
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    room,
                    bed,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return no bed when given premises has got 2 rooms where one with endDate in the passed and another room with matching end date`() {
      `Given a User` { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(1)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.name)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return bed matches searching pdu`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withName(randomStringLowerCase(8))
        withProbationRegion(probationRegion)
      }

      val pduTwo = probationDeliveryUnitFactory.produceAndPersist {
        withName(randomStringLowerCase(8))
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.parse("2024-03-12")
        val durationDays = 7
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val roomOne = roomEntityFactory.produceAndPersist {
          withName("Room One")
          withPremises(premisesOne)
        }

        val bedOne = bedEntityFactory.produceAndPersist {
          withName("Bed One")
          withEndDate { searchStartDate.plusDays(20) }
          withRoom(roomOne)
        }

        val roomTwo = roomEntityFactory.produceAndPersist {
          withName("Room Two")
          withPremises(premisesOne)
        }

        val bedTwo = bedEntityFactory.produceAndPersist {
          withName("Bed Two")
          withRoom(roomTwo)
        }

        val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val roomThree = roomEntityFactory.produceAndPersist {
          withPremises(premisesTwo)
        }

        bedEntityFactory.produceAndPersist {
          withName("Bed Three")
          withEndDate { searchStartDate.plusDays(40) }
          withRoom(roomThree)
        }

        val roomFour = roomEntityFactory.produceAndPersist {
          withPremises(premisesTwo)
        }

        bedEntityFactory.produceAndPersist {
          withName("Bed Four")
          withRoom(roomFour)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
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
                resultsPremisesCount = 1,
                resultsBedCount = 2,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomTwo,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed in multiple pdus should return bed matches searching pdus`() {
      val pduOne = probationDeliveryUnitFactory.produceAndPersist {
        withName("Probation Delivery Unit One")
        withProbationRegion(probationRegion)
      }

      val pduTwo = probationDeliveryUnitFactory.produceAndPersist {
        withName("Probation Delivery Unit Two")
        withProbationRegion(probationRegion)
      }

      val pduThree = probationDeliveryUnitFactory.produceAndPersist {
        withName("Probation Delivery Unit Three")
        withProbationRegion(probationRegion)
      }

      `Given a User`(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.parse("2024-08-12")
        val durationDays = 7
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduOne)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesOne, bedOnePremisesOne) = createBedspace(premisesOne, "Room One")
        val (roomTwoPremisesOne, bedTwoPremisesOne) = createBedspace(premisesOne, "Room Two")

        val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesTwo, bedOnePremisesTwo) = createBedspace(premisesTwo, "Room One")

        val premisesThree = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Three")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduThree)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesThree, bedOnePremisesThree) = createBedspace(premisesThree, "Room One")

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              serviceName = "temporary-accommodation",
              probationDeliveryUnit = null,
              probationDeliveryUnits = listOf(pduOne.id, pduThree.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 2,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomOnePremisesOne,
                    bedOnePremisesOne,
                    pduOne.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomTwoPremisesOne,
                    bedTwoPremisesOne,
                    pduOne.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesThree,
                    roomOnePremisesThree,
                    bedOnePremisesThree,
                    pduThree.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    private fun searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(
      jwt: String,
      searchStartDate: LocalDate,
      durationDays: Int,
      pduName: String,
    ) {
      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = searchStartDate,
            durationDays = durationDays,
            serviceName = "temporary-accommodation",
            probationDeliveryUnit = pduName,
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

    private fun createTemporaryAccommodationPremisesWithCharacteristics(
      premisesName: String,
      probationRegion: ProbationRegionEntity,
      localAuthorityArea: LocalAuthorityAreaEntity,
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      characteristics: MutableList<CharacteristicEntity>,
    ): TemporaryAccommodationPremisesEntity {
      return temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withName(premisesName)
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(probationDeliveryUnit)
        withStatus(PropertyStatus.active)
        withCharacteristics(characteristics)
      }
    }

    private fun createBedspace(premises: PremisesEntity, roomName: String): Pair<RoomEntity, BedEntity> {
      val room = roomEntityFactory.produceAndPersist {
        withName(roomName)
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName(randomStringMultiCaseWithNumbers(10))
        withRoom(room)
      }

      return Pair(room, bed)
    }

    private fun createTemporaryAccommodationWithBedSpaceEndDate(
      searchStartDate: LocalDate,
      bedEndDate: LocalDate,
    ): ProbationDeliveryUnitEntity {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
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

      bedEntityFactory.produceAndPersist {
        withName("not Matching Bed")
        withEndDate { bedEndDate }
        withRoom(room)
      }

      val roomWithoutEndDate = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      bedEntityFactory.produceAndPersist {
        withName("Not Matching Bed")
        withEndDate { searchStartDate.minusDays(5) }
        withRoom(roomWithoutEndDate)
      }
      return searchPdu
    }

    @SuppressWarnings("LongParameterList")
    private fun createTemporaryAccommodationBedSearchResult(
      premises: PremisesEntity,
      room: RoomEntity,
      bed: BedEntity,
      pduName: String,
      numberOfBeds: Int,
      numberOfBookedBeds: Int,
      premisesCharacteristics: List<CharacteristicPair>,
      overlaps: List<TemporaryAccommodationBedSearchResultOverlap>,
    ): TemporaryAccommodationBedSearchResult {
      return TemporaryAccommodationBedSearchResult(
        premises = BedSearchResultPremisesSummary(
          id = premises.id,
          name = premises.name,
          addressLine1 = premises.addressLine1,
          postcode = premises.postcode,
          probationDeliveryUnitName = pduName,
          characteristics = premisesCharacteristics,
          addressLine2 = premises.addressLine2,
          town = premises.town,
          notes = premises.notes,
          bedCount = numberOfBeds,
          bookedBedCount = numberOfBookedBeds,
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
        overlaps = overlaps,
      )
    }

    private fun createPremisesAndBedsWithCharacteristics(
      localAuthorityArea: LocalAuthorityAreaEntity,
      pdu: ProbationDeliveryUnitEntity,
      bedSearchAttribute: BedSearchAttributes,
    ): List<BedEntity> {
      val premisesSingleOccupancyCharacteristic = characteristicRepository.findByName("Single occupancy")
      val premisesSharedPropertyCharacteristic = characteristicRepository.findByName("Shared property")
      val premisesWheelchairAccessibleCharacteristic = characteristicRepository.findByName("Wheelchair accessible")
      val premisesMenOnlyCharacteristic = characteristicRepository.findByName("Men only")
      val premisesWomenOnlyCharacteristic = characteristicRepository.findByName("Women only")
      val premisesPubNearbyCharacteristic = characteristicRepository.findByName("Pub nearby")
      var beds = listOf<BedEntity>()

      val premisesSingleOccupancy = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Single Occupancy",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic!!),
      )

      val (singleOccupancyRoomOne, singleOccupancyBedOne) = createBedspace(premisesSingleOccupancy, "Premises Single Occupancy Room One")

      val premisesSharedProperty = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Shared Property",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSharedPropertyCharacteristic!!),
      )

      val (sharedPropertyRoomOne, sharedPropertyBedOne) = createBedspace(premisesSharedProperty, "Premises Shared Property Room One")

      val premisesWheelchairAccessible = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Wheelchair Accessible",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWheelchairAccessibleCharacteristic!!),
      )

      val (wheelchairAccessibleRoomOne, wheelchairAccessibleBedOne) = createBedspace(premisesWheelchairAccessible, "Premises Wheelchair Accessible Room One")

      val premisesMenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic!!),
      )

      val (premisesMenOnlyRoomOne, premisesMenOnlyBedOne) = createBedspace(premisesMenOnly, "Premises Men Only Room One")

      val premisesWomenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic!!),
      )

      val (premisesWomenOnlyRoomOne, premisesWomenOnlyBedOne) = createBedspace(premisesWomenOnly, "Premises Women Only Room One")

      val premisesPubNearby = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Pub Nearby",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesPubNearbyCharacteristic!!),
      )

      val (premisesPubNearbyRoomOne, premisesPubNearbyBedOne) = createBedspace(premisesPubNearby, "Premises Pub Nearby Room One")

      val premisesSingleOccupancyWomenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Single Occupancy - Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic, premisesSingleOccupancyCharacteristic),
      )

      val (premisesSingleOccupancyWomenOnlyRoomOne, premisesSingleOccupancyWomenOnlyBedOne) = createBedspace(premisesSingleOccupancyWomenOnly, "Premises Single Occupancy Women Only Room One")

      val premisesSharedPropertyMenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Shared Property - Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic, premisesSharedPropertyCharacteristic),
      )

      val (premisesSharedPropertyMenOnlyRoomOne, premisesSharedPropertyMenOnlyBedOne) = createBedspace(premisesSharedPropertyMenOnly, "Premises Shared Property Men Only Room One")

      val premisesSingleOccupancyWheelchairAccessible = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Single Occupancy with Wheelchair Accessible",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic, premisesWheelchairAccessibleCharacteristic),
      )

      val (premisesSingleOccupancyWheelchairAccessibleRoomOne, premisesSingleOccupancyWheelchairAccessibleBedOne) =
        createBedspace(premisesSingleOccupancyWheelchairAccessible, "Premises Single Occupancy with Wheelchair Accessible Room One")

      val premisesSharedPropertyWheelchairAccessible = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Shared Property with Wheelchair Accessible",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSharedPropertyCharacteristic, premisesWheelchairAccessibleCharacteristic),
      )

      val (premisesSharedPropertyWheelchairAccessibleRoomOne, premisesSharedPropertyWheelchairAccessibleBedOne) =
        createBedspace(premisesSharedPropertyWheelchairAccessible, "Premises Shared Property with Wheelchair Accessible Room One")

      when (bedSearchAttribute) {
        BedSearchAttributes.singleOccupancy -> beds = listOf(singleOccupancyBedOne, premisesSingleOccupancyWomenOnlyBedOne, premisesSingleOccupancyWheelchairAccessibleBedOne)
        BedSearchAttributes.sharedProperty -> beds = listOf(sharedPropertyBedOne, premisesSharedPropertyMenOnlyBedOne, premisesSharedPropertyWheelchairAccessibleBedOne)
        BedSearchAttributes.wheelchairAccessible -> beds = listOf(premisesSharedPropertyWheelchairAccessibleBedOne, premisesSingleOccupancyWheelchairAccessibleBedOne, wheelchairAccessibleBedOne)
      }
      return beds
    }
  }
}
