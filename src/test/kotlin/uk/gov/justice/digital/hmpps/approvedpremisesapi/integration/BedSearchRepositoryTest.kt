package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import java.time.LocalDate

@SuppressWarnings("LargeClass")
class BedSearchRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var bedSearchRepository: BedSearchRepository

  lateinit var probationRegion: ProbationRegionEntity
  lateinit var otherProbationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
    otherProbationRegion = givenAProbationRegion()
  }

  // Searching for a Bed returns Beds ordered by their distance from the postcode district that:
  // - Match all the characteristics at both Premises and Room level
  // - Belong to an active Premises
  // - Do not have any overlapping Bookings or Lost Beds (except where they have been cancelled)
  // - Are less than or equal to the maximum distance away from the Postcode District
  @Test
  fun `Searching for an Approved Premises Bed returns correct results`() {
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
      premisesCharacteristics[4],
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
      roomCharacteristics[3],
    )

    // Premises that doesn't match the characteristics but does match everything else
    // which has a room/bed which matches everything else - this should not be returned in the results

    val premisesWithoutAllMatchingCharacteristics = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(
        // All required characteristics except last one
        requiredPremisesCharacteristics.dropLast(1),
      )
      withLatitude(tenMilesFromPostcodeDistrict.latitude)
      withLongitude(tenMilesFromPostcodeDistrict.longitude)
      withStatus(PropertyStatus.active)
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
      withStatus(PropertyStatus.active)
    }

    val roomMatchingEverything = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    val bedOneInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning start")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedOneInRoomOneInPremisesOneMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomOneInPremisesOneMatchingEverything

    val bedTwoInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning end")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedTwoInRoomOneInPremisesOneMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-14"))
      withDepartureDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedTwoInRoomOneInPremisesOneMatchingEverything

    val bedThreeInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking in middle")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedThreeInRoomOneInPremisesOneMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-10"))
      withDepartureDate(LocalDate.parse("2023-03-12"))
    }

    bedsThatShouldNotAppearInSearchResults += bedThreeInRoomOneInPremisesOneMatchingEverything

    val bedFourInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning start and end")
      withRoom(roomMatchingEverything)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedFourInRoomOneInPremisesOneMatchingEverything)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedFourInRoomOneInPremisesOneMatchingEverything

    val bedFiveInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning start")
      withRoom(roomMatchingEverything)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedFiveInRoomOneInPremisesOneMatchingEverything)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedFiveInRoomOneInPremisesOneMatchingEverything

    val bedSixInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning end")
      withRoom(roomMatchingEverything)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedSixInRoomOneInPremisesOneMatchingEverything)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-14"))
      withEndDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedSixInRoomOneInPremisesOneMatchingEverything

    val bedSevenInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed in middle")
      withRoom(roomMatchingEverything)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedSevenInRoomOneInPremisesOneMatchingEverything)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-10"))
      withEndDate(LocalDate.parse("2023-03-12"))
    }

    bedsThatShouldNotAppearInSearchResults += bedSevenInRoomOneInPremisesOneMatchingEverything

    val bedEightInRoomOneInPremisesOneMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning start and end")
      withRoom(roomMatchingEverything)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneMatchingEverything)
      withBed(bedEightInRoomOneInPremisesOneMatchingEverything)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedEightInRoomOneInPremisesOneMatchingEverything

    // Premises that is too far away but does match everything else which has
    // a room/bed which matches everything else - this should not be returned in the results

    val premisesMatchingEverythingExceptDistance = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(requiredPremisesCharacteristics)
      withLatitude(fiftyOneMilesFromPostcodeDistrict.latitude)
      withLongitude(fiftyOneMilesFromPostcodeDistrict.longitude)
      withStatus(PropertyStatus.active)
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
        requiredRoomCharacteristics.dropLast(1),
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
      withStatus(PropertyStatus.active)
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
      withStatus(PropertyStatus.active)
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

    val cancelledLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesThreeMatchingEverything)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-10"))
    }

    cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
      withVoidBedspace(cancelledLostBed)
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

    val bedWithNonArrivedBooking = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with nonArrived booking")
      withRoom(roomMatchingEverythingInPremisesThree)
    }

    val nonArrivedBooking = bookingEntityFactory.produceAndPersist {
      withPremises(premisesThreeMatchingEverything)
      withBed(bedWithNonArrivedBooking)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    nonArrivalEntityFactory.produceAndPersist {
      withBooking(nonArrivedBooking)
      withReason(nonArrivalReasonEntityFactory.produceAndPersist())
    }

    bedsThatShouldAppearInSearchResults += bedWithNonArrivedBooking

    val nonActivePremisesMatchingEverything = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withCharacteristicsList(requiredPremisesCharacteristics)
      withLatitude(tenMilesFromPostcodeDistrict.latitude)
      withLongitude(tenMilesFromPostcodeDistrict.longitude)
      withStatus(PropertyStatus.archived)
    }

    val roomMatchingEverythingInNonActivePremisesMatchingEverything = roomEntityFactory.produceAndPersist {
      withPremises(nonActivePremisesMatchingEverything)
      withCharacteristicsList(requiredRoomCharacteristics)
    }

    val bedOneInRoomOneInNonActivePremisesMatchingEverything = bedEntityFactory.produceAndPersist {
      withName("Matching Bed in archived Premises")
      withRoom(roomMatchingEverythingInNonActivePremisesMatchingEverything)
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomOneInNonActivePremisesMatchingEverything

    val results = bedSearchRepository.findApprovedPremisesBeds(
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 50,
      startDate = LocalDate.parse("2023-03-09"),
      durationInDays = 7,
      requiredPremisesCharacteristics = requiredPremisesCharacteristics.map(CharacteristicEntity::id),
      requiredRoomCharacteristics = requiredRoomCharacteristics.map(CharacteristicEntity::id),
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

    assertThat(results.first { it.premisesId == premisesTwoMatchingEverything.id }.premisesBedCount).isEqualTo(4)
  }

  // Searching for a Bed returns Beds ordered by their distance from the postcode district that:
  // - Is part of the probation delivery unit specified
  // - Belong to an active Premises that is part of the user's Probation Region
  // - Do not have any overlapping Bookings or Lost Beds (except where they have been cancelled)
  @Test
  fun `Searching for a Temporary Accommodation Bed returns correct results`() {
    val bedsThatShouldNotAppearInSearchResults = mutableListOf<BedEntity>()
    val bedsThatShouldAppearInSearchResults = mutableListOf<BedEntity>()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    // Premises isn't in the PDU which has a room/bed which matches everything else - this should not be returned in the results

    val premisesNotInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withYieldedProbationDeliveryUnit {
        probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        }
      }
      withStatus(PropertyStatus.active)
    }

    val roomInPremisesNotInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesNotInPdu)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersist {
      withName("Bed in Room in Premises that is not in PDU")
      withRoom(roomInPremisesNotInPdu)
    }

    // Premises that is in PDU with a room
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

    val premisesOneInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
    }

    val roomInPremisesOneInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
    }

    val bedOneInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning start")
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedOneInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomInPremisesOneInPdu

    val bedTwoInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning end")
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedTwoInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2023-03-14"))
      withDepartureDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedTwoInRoomInPremisesOneInPdu

    val bedThreeInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking in middle")
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedThreeInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedThreeInRoomInPremisesOneInPdu

    val bedFourInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping booking spanning start and end")
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedFourInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedFourInRoomInPremisesOneInPdu

    val bedFiveInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning start")
      withRoom(roomInPremisesOneInPdu)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
      withBed(bedFiveInRoomInPremisesOneInPdu)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-10"))
    }

    bedsThatShouldNotAppearInSearchResults += bedFiveInRoomInPremisesOneInPdu

    val bedSixInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning end")
      withRoom(roomInPremisesOneInPdu)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
      withBed(bedSixInRoomInPremisesOneInPdu)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-14"))
      withEndDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedSixInRoomInPremisesOneInPdu

    val bedSevenInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed in middle")
      withRoom(roomInPremisesOneInPdu)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
      withBed(bedSevenInRoomInPremisesOneInPdu)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-10"))
      withEndDate(LocalDate.parse("2023-03-12"))
    }

    bedsThatShouldNotAppearInSearchResults += bedSevenInRoomInPremisesOneInPdu

    val bedEightInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with overlapping lost bed spanning start and end")
      withRoom(roomInPremisesOneInPdu)
    }

    cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
      withBed(bedEightInRoomInPremisesOneInPdu)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedEightInRoomInPremisesOneInPdu

    // Another Premises in the PDU with a Room containing 4 beds
    // which are all available during the search period

    val premisesTwoInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
    }

    val roomInPremisesTwoInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesTwoInPdu)
    }

    bedsThatShouldAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(4) {
      withName("Matching Bed in Premises Two")
      withRoom(roomInPremisesTwoInPdu)
    }

    // Another Premises that is in the PDU with a Room containing 4 beds
    // which are all available during the search period
    // one of these beds has a conflicting lost beds entry that has been cancelled (so lost bed should not prevent bed appearing in results)
    // one of these beds has a conflicting booking that has been cancelled (so booking should not prevent bed appearing in search results)

    val premisesThreeInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
    }

    val roomInPremisesThreeInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
    }

    bedsThatShouldAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(2) {
      withName("Matching bed in Premises Three")
      withRoom(roomInPremisesThreeInPdu)
    }

    val bedWithCancelledLostBed = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with cancelled lost bed")
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2023-03-08"))
      withEndDate(LocalDate.parse("2023-03-10"))
    }

    cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
      withVoidBedspace(cancelledLostBed)
    }

    bedsThatShouldAppearInSearchResults += bedWithCancelledLostBed

    val bedWithCancelledBooking = bedEntityFactory.produceAndPersist {
      withName("Matching Bed with cancelled booking")
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledBooking = bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withArrivalDate(LocalDate.parse("2023-03-08"))
      withDepartureDate(LocalDate.parse("2023-03-10"))
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(cancelledBooking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    bedsThatShouldAppearInSearchResults += bedWithCancelledBooking

    val nonActivePremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.archived)
    }

    val roomInNonActivePremises = roomEntityFactory.produceAndPersist {
      withPremises(nonActivePremises)
    }

    val bedOneInRoomOneInNonActivePremises = bedEntityFactory.produceAndPersist {
      withName("Matching Bed in archived Premises")
      withRoom(roomInNonActivePremises)
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomOneInNonActivePremises

    val premisesInOtherProbationRegion = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(otherProbationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
    }

    val roomInPremisesInOtherProbationRegion = roomEntityFactory.produceAndPersist {
      withPremises(premisesInOtherProbationRegion)
    }

    val bedOneInRoomInPremisesInOtherProbationRegion = bedEntityFactory.produceAndPersist {
      withName("Matching Bed in Premises in different Probation Region")
      withRoom(roomInPremisesInOtherProbationRegion)
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomInPremisesInOtherProbationRegion

    val results = bedSearchRepository.findTemporaryAccommodationBeds(
      probationDeliveryUnits = listOf(searchPdu.id),
      startDate = LocalDate.parse("2023-03-09"),
      endDate = LocalDate.parse("2023-03-15"),
      probationRegionId = probationRegion.id,
      listOf(),
      listOf(),
    )

    assertThat(results.size).isEqualTo(bedsThatShouldAppearInSearchResults.size)
    results.forEach { searchResult ->
      bedsThatShouldNotAppearInSearchResults.none { searchResult.bedId == it.id }
      bedsThatShouldAppearInSearchResults.any { searchResult.bedId == it.id }
    }

    assertThat(results.first { it.premisesId == premisesTwoInPdu.id }.premisesBedCount).isEqualTo(4)
    assertThat(results.first { it.premisesId == premisesTwoInPdu.id }.bookedBedCount).isEqualTo(0)
    assertThat(results.first { it.premisesId == premisesThreeInPdu.id }.premisesBedCount).isEqualTo(4)
    assertThat(results.first { it.premisesId == premisesThreeInPdu.id }.bookedBedCount).isEqualTo(0)
  }

  @Test
  fun `Searching for a Temporary Accommodation Bedspace in a Shared Property returns correct results`() {
    val bedsThatShouldNotAppearInSearchResults = mutableListOf<BedEntity>()
    val bedsThatShouldAppearInSearchResults = mutableListOf<BedEntity>()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val premisesSharedPropertyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Shared property")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesSingleOccupancyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Single occupancy")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesWomenOnlyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Women only")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesOneInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSingleOccupancyCharacteristic, premisesWomenOnlyCharacteristic))
    }

    val roomInPremisesOneInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
    }

    val bedOneInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedOneInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2024-08-08"))
      withDepartureDate(LocalDate.parse("2024-08-20"))
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomInPremisesOneInPdu

    val bedTwoInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedTwoInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2024-08-12"))
      withDepartureDate(LocalDate.parse("2024-08-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedTwoInRoomInPremisesOneInPdu

    val premisesTwoInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSharedPropertyCharacteristic, premisesWomenOnlyCharacteristic))
    }

    val roomInPremisesTwoInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesTwoInPdu)
    }

    bedsThatShouldAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(2) {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesTwoInPdu)
    }

    val bedWithBooking = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesTwoInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesTwoInPdu)
      withBed(bedWithBooking)
      withArrivalDate(LocalDate.parse("2024-08-08"))
      withDepartureDate(LocalDate.parse("2024-09-18"))
    }

    bedsThatShouldNotAppearInSearchResults += bedWithBooking

    val premisesThreeInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSharedPropertyCharacteristic))
    }

    val roomInPremisesThreeInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
    }

    bedsThatShouldAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(3) {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val bedWithCancelledLostBed = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2024-08-08"))
      withEndDate(LocalDate.parse("2024-09-10"))
    }

    cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
      withVoidBedspace(cancelledLostBed)
    }

    bedsThatShouldAppearInSearchResults += bedWithCancelledLostBed

    val bedWithCancelledBooking = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledBooking = bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withArrivalDate(LocalDate.parse("2024-08-18"))
      withDepartureDate(LocalDate.parse("2024-09-20"))
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(cancelledBooking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    bedsThatShouldAppearInSearchResults += bedWithCancelledBooking

    val nonActivePremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.archived)
    }

    val roomInNonActivePremises = roomEntityFactory.produceAndPersist {
      withPremises(nonActivePremises)
    }

    val bedOneInRoomOneInNonActivePremises = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInNonActivePremises)
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomOneInNonActivePremises

    val results = bedSearchRepository.findTemporaryAccommodationBeds(
      probationDeliveryUnits = listOf(searchPdu.id),
      startDate = LocalDate.parse("2024-08-29"),
      endDate = LocalDate.parse("2024-09-15"),
      probationRegionId = probationRegion.id,
      listOf(premisesSharedPropertyCharacteristic.id),
      listOf(),
    )

    assertThat(results.size).isEqualTo(bedsThatShouldAppearInSearchResults.size)
    results.forEach { searchResult ->
      bedsThatShouldNotAppearInSearchResults.none { searchResult.bedId == it.id }
      bedsThatShouldAppearInSearchResults.any { searchResult.bedId == it.id }
    }

    assertThat(results.first { it.premisesId == premisesTwoInPdu.id }.premisesBedCount).isEqualTo(3)
    assertThat(results.first { it.premisesId == premisesTwoInPdu.id }.bookedBedCount).isEqualTo(1)
    assertThat(results.first { it.premisesId == premisesThreeInPdu.id }.premisesBedCount).isEqualTo(5)
    assertThat(results.first { it.premisesId == premisesThreeInPdu.id }.bookedBedCount).isEqualTo(0)
  }

  @Test
  fun `Searching for a Temporary Accommodation Bedspace in a Single Occupancy Property returns correct results`() {
    val bedsThatShouldNotAppearInSearchResults = mutableListOf<BedEntity>()
    val bedsThatShouldAppearInSearchResults = mutableListOf<BedEntity>()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val premisesSingleOccupancyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Single occupancy")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesSharedPropertyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Shared property")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesWomenOnlyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Women only")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesOneInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSingleOccupancyCharacteristic, premisesWomenOnlyCharacteristic))
    }

    val roomInPremisesOneInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
    }

    val bedOneInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedOneInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2024-08-08"))
      withDepartureDate(LocalDate.parse("2024-08-20"))
    }

    bedsThatShouldAppearInSearchResults += bedOneInRoomInPremisesOneInPdu

    val bedTwoInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedTwoInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2024-08-12"))
      withDepartureDate(LocalDate.parse("2024-09-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedTwoInRoomInPremisesOneInPdu

    val premisesTwoInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSharedPropertyCharacteristic, premisesWomenOnlyCharacteristic))
    }

    val roomInPremisesTwoInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesTwoInPdu)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(2) {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesTwoInPdu)
    }

    val bedWithBooking = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesTwoInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesTwoInPdu)
      withBed(bedWithBooking)
      withArrivalDate(LocalDate.parse("2024-08-08"))
      withDepartureDate(LocalDate.parse("2024-09-18"))
    }

    bedsThatShouldNotAppearInSearchResults += bedWithBooking

    val premisesThreeInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSharedPropertyCharacteristic))
    }

    val roomInPremisesThreeInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(3) {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val bedWithCancelledLostBed = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2024-08-08"))
      withEndDate(LocalDate.parse("2024-09-10"))
    }

    cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
      withVoidBedspace(cancelledLostBed)
    }

    bedsThatShouldNotAppearInSearchResults += bedWithCancelledLostBed

    val bedWithCancelledBooking = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledBooking = bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withArrivalDate(LocalDate.parse("2024-08-18"))
      withDepartureDate(LocalDate.parse("2024-09-20"))
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(cancelledBooking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    bedsThatShouldNotAppearInSearchResults += bedWithCancelledBooking

    val nonActivePremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.archived)
    }

    val roomInNonActivePremises = roomEntityFactory.produceAndPersist {
      withPremises(nonActivePremises)
    }

    val bedOneInRoomOneInNonActivePremises = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInNonActivePremises)
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomOneInNonActivePremises

    val results = bedSearchRepository.findTemporaryAccommodationBeds(
      probationDeliveryUnits = listOf(searchPdu.id),
      startDate = LocalDate.parse("2024-08-29"),
      endDate = LocalDate.parse("2024-09-15"),
      probationRegionId = probationRegion.id,
      listOf(premisesSingleOccupancyCharacteristic.id),
      listOf(),
    )

    assertThat(results.size).isEqualTo(bedsThatShouldAppearInSearchResults.size)
    results.forEach { searchResult ->
      bedsThatShouldNotAppearInSearchResults.none { searchResult.bedId == it.id }
      bedsThatShouldAppearInSearchResults.any { searchResult.bedId == it.id }
    }

    assertThat(results.first { it.premisesId == premisesOneInPdu.id }.premisesBedCount).isEqualTo(2)
  }

  @Test
  fun `Searching for a Temporary Accommodation Bedspace in a wheelchair accessible returns correct results`() {
    val bedsThatShouldNotAppearInSearchResults = mutableListOf<BedEntity>()
    val bedsThatShouldAppearInSearchResults = mutableListOf<BedEntity>()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val premisesWheelchairAccessibleCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Wheelchair accessible")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesSharedPropertyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Shared property")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesWomenOnlyCharacteristic = characteristicEntityFactory.produceAndPersist {
      withName("Women only")
      withServiceScope("temporary-accommodation")
      withModelScope("premises")
    }

    val premisesOneInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesWheelchairAccessibleCharacteristic, premisesWomenOnlyCharacteristic))
    }

    val roomInPremisesOneInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesOneInPdu)
    }

    val bedOneInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedOneInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2024-08-08"))
      withDepartureDate(LocalDate.parse("2024-08-20"))
    }

    bedsThatShouldAppearInSearchResults += bedOneInRoomInPremisesOneInPdu

    val bedTwoInRoomInPremisesOneInPdu = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesOneInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesOneInPdu)
      withBed(bedTwoInRoomInPremisesOneInPdu)
      withArrivalDate(LocalDate.parse("2024-08-12"))
      withDepartureDate(LocalDate.parse("2024-09-16"))
    }

    bedsThatShouldNotAppearInSearchResults += bedTwoInRoomInPremisesOneInPdu

    val premisesTwoInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSharedPropertyCharacteristic, premisesWomenOnlyCharacteristic))
    }

    val roomInPremisesTwoInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesTwoInPdu)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(2) {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesTwoInPdu)
    }

    val bedWithBooking = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesTwoInPdu)
    }

    bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesTwoInPdu)
      withBed(bedWithBooking)
      withArrivalDate(LocalDate.parse("2024-08-08"))
      withDepartureDate(LocalDate.parse("2024-09-18"))
    }

    bedsThatShouldNotAppearInSearchResults += bedWithBooking

    val premisesThreeInPdu = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.active)
      withCharacteristics(mutableListOf(premisesSharedPropertyCharacteristic))
    }

    val roomInPremisesThreeInPdu = roomEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
    }

    bedsThatShouldNotAppearInSearchResults += bedEntityFactory.produceAndPersistMultiple(3) {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val bedWithCancelledLostBed = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.parse("2024-08-08"))
      withEndDate(LocalDate.parse("2024-09-10"))
    }

    cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
      withVoidBedspace(cancelledLostBed)
    }

    bedsThatShouldNotAppearInSearchResults += bedWithCancelledLostBed

    val bedWithCancelledBooking = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInPremisesThreeInPdu)
    }

    val cancelledBooking = bookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premisesThreeInPdu)
      withBed(bedWithCancelledLostBed)
      withArrivalDate(LocalDate.parse("2024-08-18"))
      withDepartureDate(LocalDate.parse("2024-09-20"))
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(cancelledBooking)
      withReason(cancellationReasonEntityFactory.produceAndPersist())
    }

    bedsThatShouldNotAppearInSearchResults += bedWithCancelledBooking

    val nonActivePremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(searchPdu)
      withStatus(PropertyStatus.archived)
    }

    val roomInNonActivePremises = roomEntityFactory.produceAndPersist {
      withPremises(nonActivePremises)
    }

    val bedOneInRoomOneInNonActivePremises = bedEntityFactory.produceAndPersist {
      withName(randomNumberChars(10))
      withRoom(roomInNonActivePremises)
    }

    bedsThatShouldNotAppearInSearchResults += bedOneInRoomOneInNonActivePremises

    val results = bedSearchRepository.findTemporaryAccommodationBeds(
      probationDeliveryUnits = listOf(searchPdu.id),
      startDate = LocalDate.parse("2024-08-29"),
      endDate = LocalDate.parse("2024-09-15"),
      probationRegionId = probationRegion.id,
      listOf(premisesWheelchairAccessibleCharacteristic.id),
      listOf(),
    )

    assertThat(results.size).isEqualTo(bedsThatShouldAppearInSearchResults.size)
    results.forEach { searchResult ->
      bedsThatShouldNotAppearInSearchResults.none { searchResult.bedId == it.id }
      bedsThatShouldAppearInSearchResults.any { searchResult.bedId == it.id }
    }

    assertThat(results.first { it.premisesId == premisesOneInPdu.id }.premisesBedCount).isEqualTo(2)
    assertThat(results.first { it.premisesId == premisesOneInPdu.id }.bookedBedCount).isEqualTo(1)
  }
}

data class LatLong(
  val latitude: Double,
  val longitude: Double,
) {
  fun plusLatitudeMiles(miles: Int): LatLong {
    return LatLong(
      latitude = latitude + ((1.0 / 69.0) * miles),
      longitude = longitude,
    )
  }
}
