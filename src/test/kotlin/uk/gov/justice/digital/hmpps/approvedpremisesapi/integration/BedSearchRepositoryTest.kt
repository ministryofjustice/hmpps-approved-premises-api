package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import net.sf.geographiclib.Geodesic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.overlaps
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDouble
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BedSearchRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var bedSearchRepository: BedSearchRepository

  @Test
  // Searching for a Bed returns Beds ordered by their distance from the postcode district that:
  // - Match all the characteristics at both Premises and Room level
  // - Do not have any overlapping Bookings or Lost Beds (except where they have been cancelled)
  // - Are less than or equal to the maximum distance away from the Postcode District
  fun `Searching for a bed returns correct results`() {
    val postCodeDistrictLatLong = LatLong(50.1044, -2.3992)
    val tenMilesFromPostcodeDistrict = postCodeDistrictLatLong.plusLatitudeMiles(10)
    val fiftyMilesFromPostcodeDistrict = postCodeDistrictLatLong.plusLatitudeMiles(50)
    val fiftyOneMilesFromPostcodeDistrict = postCodeDistrictLatLong.plusLatitudeMiles(51)

    val bedsThatShouldNotAppearInSearchResults = mutableListOf<BedEntity>()
    val bedsThatShouldAppearInSearchResults = mutableListOf<BedEntity>()

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

    val premisesCharacteristics = (1..5).map { characteristicId ->
      characteristicEntityFactory.produceAndPersist {
        withPropertyName("premisesCharacteristic$characteristicId")
        withName("Premises Characteristic $characteristicId")
        withServiceScope("approved-premises")
        withModelScope("premises")
      }
    }

    val requiredPremisesCharacteristics = listOf(
      premisesCharacteristics[0],
      premisesCharacteristics[2],
      premisesCharacteristics[4]
    )

    val roomCharacteristics = (1..5).map { characteristicId ->
      characteristicEntityFactory.produceAndPersist {
        withPropertyName("roomCharacteristic$characteristicId")
        withName("Room Characteristic $characteristicId")
        withServiceScope("approved-premises")
        withModelScope("room")
      }
    }

    val requiredRoomCharacteristics = listOf(
      roomCharacteristics[1],
      roomCharacteristics[2],
      roomCharacteristics[3]
    )

    // Premises that doesn't match the characteristics but does match everything else
    // which has a room/bed which matches everything else - this should not be returned in the results

    val premisesWithoutAllMatchingCharacteristics = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(
        // All required characteristics except last one
        requiredPremisesCharacteristics.dropLast(1)
      )
      withLatitude(tenMilesFromPostcodeDistrict.latitude)
      withLongitude(tenMilesFromPostcodeDistrict.longitude)
    }

    val roomWithAllMatchingCharacteristicsInPremisesWithoutAllCharacteristics = roomEntityFactory.produceAndPersist {
      withPremises(premisesWithoutAllMatchingCharacteristics)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersist {
      withName("Bed in Room which matches but Premises that does not")
      withRoom(roomWithAllMatchingCharacteristicsInPremisesWithoutAllCharacteristics)
    }

    // Premises that matches everything and has a room which matches everything
    // the room has 4 beds which each have an overlapping booking:
    // - a booking that overlaps the start of the period we're searching for
    // - a booking that overlaps part of the middle of the period we're searching for
    // - a booking that overlaps the end of the period we're searching for
    // - a booking that overlaps the entirety of the period we're searching for
    //   |-------------------[    search period    ]--------------------------------------|
    //   |--------------[booking 1]-------------------------------------------------------|
    //   |---------------------------[booking 2]------------------------------------------|
    //   |------------------------------------[booking 3]---------------------------------|
    //   |---------------[          booking 4           ]---------------------------------|
    // The room has 4 more beds which each have a Lost Beds entry for the same periods as above

    val premisesOneMatchingEverything = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(requiredPremisesCharacteristics)
      withLatitude(tenMilesFromPostcodeDistrict.latitude)
      withLongitude(tenMilesFromPostcodeDistrict.longitude)
    }

    val roomMatchingEverything = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    val bedOneInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning start")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedOneInRoomMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomMatchingEverything

    val bedTwoInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning end")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedTwoInRoomMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-10"))
      withDepartureDate(LocalDate.parse("2023-03-12"))
    }

    bedsThatShouldNotAppearInSearchResults += bedTwoInRoomMatchingEverything

    val bedThreeInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking in middle")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedThreeInRoomMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-15"))
      withDepartureDate(LocalDate.parse("2023-03-17"))
    }

    bedsThatShouldNotAppearInSearchResults += bedThreeInRoomMatchingEverything

    val bedFourInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning start and end")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedFourInRoomMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-17"))
    }

    bedsThatShouldNotAppearInSearchResults += bedFourInRoomMatchingEverything

    val bedFiveInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning start")
      withRoom(roomMatchingEverything)
    }

    lostBedsEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedFiveInRoomMatchingEverything)
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedFiveInRoomMatchingEverything

    val bedSixInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning end")
      withRoom(roomMatchingEverything)
    }

    lostBedsEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedSixInRoomMatchingEverything)
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-10"))
      withEndDate(LocalDate.parse("2023-03-12"))
    }

    bedsThatShouldNotAppearInSearchResults += bedSixInRoomMatchingEverything

    val bedSevenInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed in middle")
      withRoom(roomMatchingEverything)
    }

    lostBedsEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedSevenInRoomMatchingEverything)
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-15"))
      withEndDate(LocalDate.parse("2023-03-17"))
    }

    bedsThatShouldNotAppearInSearchResults += bedSevenInRoomMatchingEverything

    val bedEightInRoomMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning start and end")
      withRoom(roomMatchingEverything)
    }

    lostBedsEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedEightInRoomMatchingEverything)
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-17"))
    }

    bedsThatShouldNotAppearInSearchResults += bedEightInRoomMatchingEverything

    // Premises that is too far away but does match everything else which has
    // a room/bed which matches everything else - this should not be returned in the results

    val premisesMatchingEverythingExceptDistance = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(requiredPremisesCharacteristics)
      withLatitude(fiftyOneMilesFromPostcodeDistrict.latitude)
      withLongitude(fiftyOneMilesFromPostcodeDistrict.longitude)
    }

    val roomMatchingEverythingInPremisesTooFarAway = roomEntityFactory.produceAndPersist {
      withPremises(premisesMatchingEverythingExceptDistance)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersist {
      withName("Bed that matches everything except distance (too far away)")
      withRoom(roomMatchingEverythingInPremisesTooFarAway)
    }

    // Room/bed that does not match all characteristics in Premises that does -
    // this should not be returned in results

    val roomNotMatchingEverything = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withCharacteristicsList(
        // All required characteristics except last one
        requiredRoomCharacteristics.dropLast(1)
      )
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersist {
      withName("Bed in Room that doesn't match all characteristics but Premises that does")
      withRoom(roomNotMatchingEverything)
    }

    // A Premises that is ten miles away which matches everything with a Room containing 4 beds
    // which are all available during the search period

    val premisesTwoMatchingEverything = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(requiredPremisesCharacteristics)
      withLatitude(tenMilesFromPostcodeDistrict.latitude)
      withLongitude(tenMilesFromPostcodeDistrict.longitude)
    }

    val roomMatchingEverythingInPremisesTwo = roomEntityFactory.produceAndPersist {
      withPremises(premisesTwoMatchingEverything)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    bedsThatShouldAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(4) {
      withName("Matching Bed ten miles away")
      withRoom(roomMatchingEverythingInPremisesTwo)
    }

    // A Premises that is fifty miles away which matches everything with a Room containing 4 beds
    // which are all available during the search period
    // one of these beds has a conflicting lost beds entry that has been cancelled (so lost bed should not prevent bed appearing in results)
    // one of these beds has a conflicting booking that has been cancelled (so booking should not prevent bed appearing in search results)

    val premisesThreeMatchingEverything = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(requiredPremisesCharacteristics)
      withLatitude(fiftyMilesFromPostcodeDistrict.latitude)
      withLongitude(fiftyMilesFromPostcodeDistrict.longitude)
    }

    val roomMatchingEverythingInPremisesThree = roomEntityFactory.produceAndPersist {
      withPremises(premisesThreeMatchingEverything)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    bedsThatShouldAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(2) {
      withName("Matching bed fifty miles away")
      withRoom(roomMatchingEverythingInPremisesThree)
    }

    val bedWithCancelledLostBed = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with cancelled lost bed")
      withRoom(roomMatchingEverythingInPremisesThree)
    }

    val cancelledLostBed = lostBedsEntityFactory.produceAndPersist {
      withPremises(premisesThreeMatchingEverything)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-10"))
    }

    lostBedCancellationsEntityFactory.produceAndPersist {
      withLostBed(cancelledLostBed)
    }

    bedsThatShouldAppearInSearchResults += bedWithCancelledLostBed

    val bedWithCancelledBooking = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with cancelled booking")
      withRoom(roomMatchingEverythingInPremisesThree)
    }

    val cancelledBooking = bookingEntityFactory.produceAndPersist {
      withPremises(premisesThreeMatchingEverything)
      withBed(bedWithCancelledLostBed)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(cancelledBooking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    bedsThatShouldAppearInSearchResults += bedWithCancelledBooking

    println("Wanted Premises characteristics: ${requiredPremisesCharacteristics.joinToString(", ") { "'${it.id}'" }}")
    println("Wanted Room characteristics: ${requiredRoomCharacteristics.joinToString(", ") { "'${it.id}'" }}")
    println("Want following beds in search results: ${bedsThatShouldAppearInSearchResults.joinToString(", ") { "'${it.id}'" }}")
    println("Don't want following beds in search results: ${bedsThatShouldNotAppearInSearchResults.joinToString(", ") { "'${it.id}'" }}")

    val results = bedSearchRepository.findBeds(
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 50,
      startDate = LocalDate.parse("2023-03-09"),
      durationInDays = 7,
      requiredPremisesCharacteristics = requiredPremisesCharacteristics.map(CharacteristicEntity::id),
      requiredRoomCharacteristics = requiredRoomCharacteristics.map(CharacteristicEntity::id),
      service = "approved-premises"
    )

    assertThat(results.size).isEqualTo(bedsThatShouldAppearInSearchResults.size)
    results.forEach { searchResult ->
      bedsThatShouldNotAppearInSearchResults.none { searchResult.bedId == it.id }
      bedsThatShouldAppearInSearchResults.any { searchResult.bedId == it.id }
    }

    var currentDistance = results[0].distance
    results.forEach {
      if (it.distance < currentDistance) {
        throw RuntimeException("Distance decreased in later search result - therefore results are ordered incorrectly")
      }

      currentDistance = it.distance
    }
  }

  @ParameterizedTest
  @Disabled("Long running/resource heavy")
  @CsvSource(
    value = [
      "101,25,12,34,25,10,3,84,0.75,7",
      "101,25,12,34,25,10,3,84,0.75,15",
      "101,25,12,34,25,10,3,84,0.75,30",
      "101,25,12,34,25,10,3,84,0.75,60",
      "101,25,12,34,25,10,3,84,0.75,180",
      "101,25,12,34,25,10,3,84,0.75,360",
    ]
  )
  fun `Query performance with realistic amount of data`(
    numberOfApsToCreate: Int,
    apsWithinDistanceToCreate: Int,
    characteristicSuitableApsWithinDistanceToCreate: Int,
    characteristicSuitableApsOutsideDistanceToCreate: Int,
    bedsPerApToCreate: Int,
    characteristicSuitableBedsPerApToCreate: Int,
    availableCharacteristicSuitableBedsPerApToCreate: Int,
    bookingDurationDaysToCreate: Int,
    occupancyProbabilityToCreate: Double,
    generateBookingsEitherSideOfSearchDays: Int
  ) {
    val apsOutsideOfDistanceToCreate = numberOfApsToCreate - apsWithinDistanceToCreate
    val characteristicUnsuitableApsWithinDistanceToCreate = apsWithinDistanceToCreate - characteristicSuitableApsWithinDistanceToCreate
    val characteristicUnsuitableApsOutsideDistanceToCreate = apsOutsideOfDistanceToCreate - characteristicSuitableApsOutsideDistanceToCreate
    val characteristicUnsuitableBedsPerApToCreate = bedsPerApToCreate - characteristicSuitableBedsPerApToCreate
    val unavailableCharacteristicSuitableBedsPerApToCreate = characteristicSuitableBedsPerApToCreate - availableCharacteristicSuitableBedsPerApToCreate

    val searchWindowDays = 12 * 7
    val searchWindowStart = LocalDate.parse("2023-03-15")
    val searchWindowEnd = searchWindowStart.plusDays(searchWindowDays.toLong())

    val generateBookingsWindowStart = searchWindowStart.minusDays(generateBookingsEitherSideOfSearchDays.toLong())
    val generateBookingsWindowEnd = searchWindowEnd.plusDays(generateBookingsEitherSideOfSearchDays.toLong())
    val totalDaysToPopulateWithGeneratedBookings = ChronoUnit.DAYS.between(generateBookingsWindowStart, generateBookingsWindowEnd)
    val probabilityOfBookingStartingOnDay = ((totalDaysToPopulateWithGeneratedBookings / bookingDurationDaysToCreate) * occupancyProbabilityToCreate) / 100
    println("Probability of Booking starting on day is: $probabilityOfBookingStartingOnDay")

    val postcodeDistrictLatLong = LatLong(52.917572, -1.181414)

    val postcodeDistrict = postCodeDistrictFactory.produceAndPersist {
      withOutcode("AA11")
      withLatitude(postcodeDistrictLatLong.latitude)
      withLongitude(postcodeDistrictLatLong.longitude)
    }

    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val premisesCharacteristics = (1..20).map { characteristicId ->
      characteristicEntityFactory.produceAndPersist {
        withPropertyName("premisesCharacteristic$characteristicId")
        withName("Premises Characteristic $characteristicId")
        withServiceScope("approved-premises")
        withModelScope("premises")
      }
    }

    val requiredPremisesCharacteristics = listOf(
      premisesCharacteristics[2],
      premisesCharacteristics[8],
      premisesCharacteristics[16]
    )

    val roomCharacteristics = (1..20).map { characteristicId ->
      characteristicEntityFactory.produceAndPersist {
        withPropertyName("roomCharacteristic$characteristicId")
        withName("Room Characteristic $characteristicId")
        withServiceScope("approved-premises")
        withModelScope("room")
      }
    }

    val requiredRoomCharacteristics = listOf(
      roomCharacteristics[1],
      roomCharacteristics[5],
      roomCharacteristics[19]
    )

    fun createBedsWithCharacteristics(amount: Int, premisesEntity: ApprovedPremisesEntity, characteristics: List<CharacteristicEntity>, createInSearchWindow: Boolean?) = (1..amount).map {
      val room = roomEntityFactory.produceAndPersist {
        withPremises(premisesEntity)
        withCharacteristicsList(characteristics)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      var day = generateBookingsWindowStart
      var lastBooking: BookingEntity? = null
      var occupiedDays = 0
      while (day != generateBookingsWindowEnd) {
        val bookingEndDay = day.plusDays(bookingDurationDaysToCreate.toLong())
        val lastBookingStillActiveOnThisDay = lastBooking != null && lastBooking.arrivalDate.rangeTo(lastBooking.departureDate).contains(day)
        val inSearchWindow = searchWindowStart.rangeTo(searchWindowEnd).overlaps(day.rangeTo(bookingEndDay))

        if (!lastBookingStillActiveOnThisDay && ((inSearchWindow && createInSearchWindow == true) || (inSearchWindow && randomDouble(0.0, 1.0) <= probabilityOfBookingStartingOnDay && createInSearchWindow != false) || (!inSearchWindow && randomDouble(0.0, 1.0) <= probabilityOfBookingStartingOnDay))) {
          lastBooking = bookingEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(bed.room.premises)
            withArrivalDate(day)
            withDepartureDate(bookingEndDay)
          }

          occupiedDays += 1
        }

        if (lastBookingStillActiveOnThisDay) {
          occupiedDays += 1
        }

        day = day.plusDays(1)
      }

      bed
    }

    fun createCharacteristicUnsuitableBeds(premisesEntity: ApprovedPremisesEntity): List<BedEntity> =
      createBedsWithCharacteristics(characteristicUnsuitableBedsPerApToCreate, premisesEntity, requiredRoomCharacteristics.dropLast(1), null)

    fun createAvailableCharacteristicSuitableBeds(premisesEntity: ApprovedPremisesEntity): List<BedEntity> =
      createBedsWithCharacteristics(characteristicUnsuitableBedsPerApToCreate, premisesEntity, requiredRoomCharacteristics, false)

    fun createUnavailableCharacteristicSuitableBeds(premisesEntity: ApprovedPremisesEntity): List<BedEntity> =
      createBedsWithCharacteristics(unavailableCharacteristicSuitableBedsPerApToCreate, premisesEntity, requiredRoomCharacteristics, true)

    val characteristicUnsuitableApsWithinDistance = (1..characteristicUnsuitableApsWithinDistanceToCreate).map {
      timedWithMessage("Generating characteristicUnsuitableApsWithinDistance (1/4) Premises $it/$characteristicUnsuitableApsWithinDistanceToCreate") {
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withCharacteristicsList(
            // All required characteristics except last one
            requiredPremisesCharacteristics.dropLast(1)
          )
          val latLong = randomLatLongInRadius(postcodeDistrictLatLong, 50)
          withLatitude(latLong.latitude)
          withLongitude(latLong.longitude)
        }

        createCharacteristicUnsuitableBeds(premises)
        createAvailableCharacteristicSuitableBeds(premises)
        createUnavailableCharacteristicSuitableBeds(premises)

        premises
      }
    }

    val characteristicUnsuitableApsOutsideDistance = (1..characteristicUnsuitableApsOutsideDistanceToCreate).map {
      timedWithMessage("Generating characteristicUnsuitableApsOutsideDistance (2/4) Premises $it/$characteristicUnsuitableApsOutsideDistanceToCreate") {
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withCharacteristicsList(
            // All required characteristics except last one
            requiredPremisesCharacteristics.dropLast(1)
          )
          val latLong = randomLatLongNotInRadius(postcodeDistrictLatLong, 50, 600)
          withLatitude(latLong.latitude)
          withLongitude(latLong.longitude)
        }

        createCharacteristicUnsuitableBeds(premises)
        createAvailableCharacteristicSuitableBeds(premises)
        createUnavailableCharacteristicSuitableBeds(premises)

        premises
      }
    }

    val characteristicSuitableApsOutsideDistance = (1..characteristicSuitableApsOutsideDistanceToCreate).map {
      timedWithMessage("Generating characteristicSuitableApsOutsideDistance (3/4) Premises $it/$characteristicSuitableApsOutsideDistanceToCreate") {
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withCharacteristicsList(requiredPremisesCharacteristics)
          val latLong = randomLatLongNotInRadius(postcodeDistrictLatLong, 50, 600)
          withLatitude(latLong.latitude)
          withLongitude(latLong.longitude)
        }

        createCharacteristicUnsuitableBeds(premises)
        createAvailableCharacteristicSuitableBeds(premises)
        createUnavailableCharacteristicSuitableBeds(premises)

        premises
      }
    }

    val characteristicSuitableApsWithinDistance = (1..characteristicSuitableApsWithinDistanceToCreate).map {
      timedWithMessage("Generating characteristicSuitableApsWithinDistance (4/4) Premises $it/$characteristicSuitableApsWithinDistanceToCreate") {
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withCharacteristicsList(requiredPremisesCharacteristics)
          val latLong = randomLatLongInRadius(postcodeDistrictLatLong, 50)
          withLatitude(latLong.latitude)
          withLongitude(latLong.longitude)
        }

        createCharacteristicUnsuitableBeds(premises)
        createAvailableCharacteristicSuitableBeds(premises)
        createUnavailableCharacteristicSuitableBeds(premises)

        premises
      }
    }

    val results = timedWithMessage("Searching for Bed") {
      bedSearchRepository.findBeds(
        postcodeDistrictOutcode = postcodeDistrict.outcode,
        maxDistanceMiles = 50,
        startDate = searchWindowStart,
        durationInDays = searchWindowDays,
        requiredPremisesCharacteristics = requiredPremisesCharacteristics.map(CharacteristicEntity::id),
        requiredRoomCharacteristics = requiredRoomCharacteristics.map(CharacteristicEntity::id),
        service = "approved-premises"
      )
    }

    println("Found ${results.size} Suitable Beds")
  }

  fun randomLatLongInRadius(point: LatLong, radiusMiles: Int): LatLong {
    val radiusMeters = radiusMiles * 1609.34

    val random = Random()

    val radiusInDegrees = radiusMeters / 111000f
    val u: Double = random.nextDouble()
    val v: Double = random.nextDouble()
    val w = radiusInDegrees * sqrt(u)
    val t = 2 * Math.PI * v
    val x = w * cos(t)
    val y = w * sin(t)

    val newX = x / cos(Math.toRadians(point.latitude))
    val foundLongitude = newX + point.latitude
    val foundLatitude = y + point.longitude

    return LatLong(
      foundLongitude,
      foundLatitude
    )
  }

  fun randomLatLongNotInRadius(point: LatLong, excludedRadiusMiles: Int, maxRadiusMiles: Int): LatLong {
    var otherPoint: LatLong
    while (true) {
      otherPoint = randomLatLongInRadius(point, maxRadiusMiles)

      val distanceMeters = Geodesic.WGS84.Inverse(point.latitude, point.longitude, otherPoint.latitude, otherPoint.longitude).s12

      val distanceMiles = distanceMeters / 1609.34
      if (distanceMiles > excludedRadiusMiles) break
    }

    return otherPoint
  }

  private fun <T> timedWithMessage(message: String, block: () -> T): T {
    val start = System.currentTimeMillis()

    val result = block()

    println("$message - ${(System.currentTimeMillis() - start)}ms")

    return result
  }
}

data class LatLong(
  val latitude: Double,
  val longitude: Double
) {
  fun plusLatitudeMiles(miles: Int): LatLong {
    return LatLong(
      latitude = latitude + ((1.0 / 69.0) * miles),
      longitude = longitude
    )
  }

  override fun toString() = "$latitude, $longitude"
}
