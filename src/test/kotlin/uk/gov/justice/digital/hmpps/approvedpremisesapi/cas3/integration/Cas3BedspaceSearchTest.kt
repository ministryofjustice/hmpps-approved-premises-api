package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremisesComplete
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremisesWithRoomsAndBeds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.BedspaceFilters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.PremisesFilters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_MEN_ONLY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_PUB_NEAR_BY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_SHARED_PROPERTY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_WOMEN_ONLY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

@SuppressWarnings("LargeClass")
class Cas3BedspaceSearchTest : IntegrationTestBase() {

  lateinit var probationRegion: ProbationRegionEntity

  companion object Constants {
    const val PREMISES_SINGLE_OCCUPANCY_ID = "007c8237-b4f4-4ee0-a9e6-62dc0d1c6a2c"
    const val PREMISES_SHARED_PROPERTY_ID = "e7e492d7-7853-48a8-9a98-23f68af92a85"
    const val PREMISES_MEN_ONLY_ID = "f21a3f74-0538-4541-8b29-3cbdcd240ae4"
    const val PREMISES_WOMEN_ONLY_ID = "25ff5695-eab1-46ad-9f3f-639edb180672"
    const val PREMISES_PUB_NEARBY_ID = "6a952536-d88c-4e14-be24-d80eb754d12b"
    const val PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID = "f2a0b5d1-3c4e-4b8c-8f6d-7a9e0b1f2c3d"
    const val PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID = "1735874f-ab84-479c-b007-dd49d54ab339"
    const val PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID = "df702555-6e5e-4e28-8b83-c7bdd8a8db1a"
    const val PREMISES_SHARED_PROPERTY_MEN_ONLY_ID = "a7540305-dd6e-41f3-8647-c45e7cfe7e0c"
    const val PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID = "b2a7306c-7049-41b0-b0d9-fd23bd0f7d42"
  }

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class BedspaceSearchForPremises {
    @Test
    fun `Searching for a bedspace without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas3/bedspaces/search")
        .bodyValue(
          Cas3BedspaceSearchParameters(
            startDate = LocalDate.now().plusDays(7),
            durationDays = 7,
            probationDeliveryUnits = listOf(UUID.randomUUID()),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Searching for a bedspace returns 200 with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val searchPdu = premises.probationDeliveryUnit!!
          webTestClient.post()
            .uri("cas3/bedspaces/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3BedspaceSearchParameters(
                startDate = LocalDate.now().minusDays(10),
                durationDays = 7,
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas3BedspaceSearchResults(
                  resultsRoomCount = 1,
                  resultsPremisesCount = 1,
                  resultsBedCount = 1,
                  results = listOf(
                    createBedspaceSearchResult(
                      premises,
                      bed.room,
                      bed,
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Searching for a bedspace returns an upcoming bedspace in the schedule to unarchive premises when the bedspace is online within the search range`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().plusDays(3),
        premisesEndDate = null,
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().plusDays(3),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val bedspace = bedspaces.first()
        val searchPdu = premises.probationDeliveryUnit!!
        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(7),
              durationDays = 7,
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    bedspace.room,
                    bedspace,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace will not return an upcoming bedspace with a start date after the search range start date`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(90),
        premisesEndDate = null,
        premisesStatus = PropertyStatus.active,
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().plusDays(3),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val searchPdu = premises.probationDeliveryUnit!!
        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(1),
              durationDays = 7,
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
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
    fun `Searching for a bedspace returns results that do not include bedspaces with current turnarounds`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
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

        val turnaround = cas3TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(2)
        }

        booking.turnarounds = mutableListOf(turnaround)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        searchCas3BedspaceAndAssertNoAvailability(
          jwt,
          LocalDate.parse("2023-03-23"),
          7,
          searchPdu.id,
        )
      }
    }

    @Test
    fun `Searching for a bedspace returns results when existing booking departure date is same as search start date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
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

        val turnaround = cas3TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(2)
        }

        booking.turnarounds = mutableListOf(turnaround)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        searchCas3BedspaceAndAssertNoAvailability(
          jwt,
          LocalDate.parse("2023-03-21"),
          7,
          searchPdu.id,
        )
      }
    }

    @ParameterizedTest
    @CsvSource("true,cas3/bedspaces/search", "false,cas3/bedspaces/search")
    fun `Searching for a bedspace returns results which include overlapping bookings for rooms in the same premises`(sexualRisk: Boolean, baseUrl: String) {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(4).toList()

          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

          val applications = mutableListOf<TemporaryAccommodationApplicationEntity>()
          val assessments = mutableListOf<AssessmentEntity>()

          offenders.mapIndexed { _, (offenderDetails, _) ->
            val (application, assessment) = createAssessment(user, offenderDetails.otherIds.crn, sexualRisk = sexualRisk)
            applications += application
            assessments += assessment
          }

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

          val roomThree = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val roomFour = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bedOne = bedEntityFactory.produceAndPersist {
            withName("matching bed")
            withRoom(roomOne)
          }

          val bedTwo = bedEntityFactory.produceAndPersist {
            withName("matching bed, but with an overlapping booking")
            withRoom(roomTwo)
          }

          val bedThree = bedEntityFactory.produceAndPersist {
            withName("bed in a different room, with an overlapping booking")
            withRoom(roomThree)
          }

          val bedFour = bedEntityFactory.produceAndPersist {
            withName("bed in a different room, with an overlapping booking")
            withRoom(roomFour)
          }

          val fullPersonOffenderDetails = offenders.first().first
          val fullPersonApplication = applications.first()
          val fullPersonAssessment = assessments.first()
          val fullPersonCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(fullPersonOffenderDetails)
            .withPnc(fullPersonOffenderDetails.otherIds.pncNumber)
            .produce()

          val overlappingBookingSameRoom = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(fullPersonApplication)
            withPremises(premises)
            withBed(bedTwo)
            withArrivalDate(LocalDate.now().minusDays(17))
            withDepartureDate(LocalDate.now().plusDays(14))
            withCrn(fullPersonCaseSummary.crn)
            withId(UUID.randomUUID())
          }

          val currentRestrictionOffenderDetails = offenders.drop(1).first().first
          val currentRestrictionApplication = applications.drop(1).first()
          val currentRestrictionAssessment = assessments.drop(1).first()
          val currentRestrictionCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(currentRestrictionOffenderDetails)
            .withPnc(currentRestrictionOffenderDetails.otherIds.pncNumber)
            .withCurrentRestriction(true)
            .produce()

          val currentRestrictionOverlappingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(currentRestrictionApplication)
            withPremises(premises)
            withBed(bedThree)
            withArrivalDate(LocalDate.now().plusDays(26))
            withDepartureDate(LocalDate.now().plusDays(43))
            withCrn(currentRestrictionCaseSummary.crn)
            withId(UUID.randomUUID())
          }

          val userExcludedOffenderDetails = offenders.drop(2).first().first
          val userExcludedApplication = applications.drop(2).first()
          val userExcludedAssessment = assessments.drop(2).first()
          val userExcludedCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(userExcludedOffenderDetails)
            .withPnc(userExcludedOffenderDetails.otherIds.pncNumber)
            .withCurrentExclusion(true)
            .produce()

          val userExcludedOverlappingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(userExcludedApplication)
            withPremises(premises)
            withBed(bedFour)
            withArrivalDate(LocalDate.now().plusDays(24))
            withDepartureDate(LocalDate.now().plusDays(55))
            withCrn(userExcludedCaseSummary.crn)
            withId(UUID.randomUUID())
          }

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(fullPersonCaseSummary, userExcludedCaseSummary, currentRestrictionCaseSummary))

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(userExcludedCaseSummary.crn)
                .withUserExcluded(true)
                .produce(),
              CaseAccessFactory()
                .withCrn(currentRestrictionCaseSummary.crn)
                .withUserRestricted(true)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.post()
            .uri(baseUrl)
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3BedspaceSearchParameters(
                startDate = LocalDate.now(),
                durationDays = 31,
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas3BedspaceSearchResults(
                  resultsRoomCount = 1,
                  resultsPremisesCount = 1,
                  resultsBedCount = 1,
                  results = listOf(
                    createBedspaceSearchResult(
                      premises,
                      roomOne,
                      bedOne,
                      searchPdu.name,
                      numberOfBeds = 4,
                      numberOfBookedBeds = 3,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      listOf(
                        Cas3BedspaceSearchResultOverlap(
                          name = "${fullPersonCaseSummary.name.forename} ${fullPersonCaseSummary.name.surname}",
                          crn = fullPersonCaseSummary.crn,
                          personType = PersonType.fullPerson,
                          sex = fullPersonCaseSummary.gender!!,
                          days = 15,
                          bookingId = overlappingBookingSameRoom.id,
                          roomId = roomTwo.id,
                          assessmentId = fullPersonAssessment.id,
                          isSexualRisk = sexualRisk,
                        ),
                        Cas3BedspaceSearchResultOverlap(
                          name = "Limited Access Offender",
                          crn = currentRestrictionCaseSummary.crn,
                          personType = PersonType.restrictedPerson,
                          days = 5,
                          bookingId = currentRestrictionOverlappingBooking.id,
                          roomId = roomThree.id,
                          assessmentId = currentRestrictionAssessment.id,
                          isSexualRisk = sexualRisk,
                        ),
                        Cas3BedspaceSearchResultOverlap(
                          name = "Limited Access Offender",
                          crn = userExcludedCaseSummary.crn,
                          personType = PersonType.restrictedPerson,
                          days = 7,
                          bookingId = userExcludedOverlappingBooking.id,
                          roomId = roomFour.id,
                          assessmentId = userExcludedAssessment.id,
                          isSexualRisk = sexualRisk,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Searching for a bedspace returns results which include overlapping bookings across multiple premises`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

          val (application, assessment) = createAssessment(user, offenderDetails.otherIds.crn)

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

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
            withApplication(application)
            withPremises(premisesOne)
            withBed(overlappingBedInPremisesOne)
            withArrivalDate(LocalDate.now().minusDays(19))
            withDepartureDate(LocalDate.now().plusDays(16))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          val overlappingBookingForBedInPremisesTwo = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesTwo)
            withBed(overlappingBedInPremisesTwo)
            withArrivalDate(LocalDate.now().plusDays(26))
            withDepartureDate(LocalDate.now().plusDays(57))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          val cancelledOverlappingBookingForBedInPremisesTwo = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesTwo)
            withBed(overlappingBedInPremisesTwo)
            withArrivalDate(LocalDate.now().minusDays(9))
            withDepartureDate(LocalDate.now().plusDays(7))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(cancelledOverlappingBookingForBedInPremisesTwo)
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.post()
            .uri("cas3/bedspaces/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3BedspaceSearchParameters(
                startDate = LocalDate.now().plusDays(2),
                durationDays = 31,
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas3BedspaceSearchResults(
                  resultsRoomCount = 2,
                  resultsPremisesCount = 2,
                  resultsBedCount = 2,
                  results = listOf(
                    createBedspaceSearchResult(
                      premisesOne,
                      roomInPremisesOne,
                      matchingBedInPremisesOne,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      listOf(
                        Cas3BedspaceSearchResultOverlap(
                          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
                          crn = overlappingBookingForBedInPremisesOne.crn,
                          personType = PersonType.fullPerson,
                          sex = caseSummary.gender!!,
                          days = 15,
                          bookingId = overlappingBookingForBedInPremisesOne.id,
                          roomId = roomInPremisesOne.id,
                          assessmentId = assessment.id,
                          isSexualRisk = false,
                        ),
                      ),
                    ),
                    createBedspaceSearchResult(
                      premisesTwo,
                      roomInPremisesTwo,
                      matchingBedInPremisesTwo,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(
                        Cas3BedspaceSearchResultOverlap(
                          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
                          crn = overlappingBookingForBedInPremisesTwo.crn,
                          personType = PersonType.fullPerson,
                          sex = caseSummary.gender!!,
                          days = 7,
                          bookingId = overlappingBookingForBedInPremisesTwo.id,
                          roomId = roomInPremisesTwo.id,
                          assessmentId = assessment.id,
                          isSexualRisk = false,
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
    }

    @Test
    fun `Searching for a bedspace returns results which do not include non-overlapping bookings`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
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
          withArrivalDate(LocalDate.now().plusDays(60))
          withDepartureDate(LocalDate.now().plusDays(90))
          withCrn(randomStringMultiCaseWithNumbers(16))
          withId(UUID.randomUUID())
        }

        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(7),
              durationDays = 31,
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 2,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premises,
                    roomOne,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
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
    fun `Searching for a bedspace returns results which do not consider cancelled bookings as overlapping`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

          val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          }

          val (application, _) = createAssessment(user, offenderDetails.otherIds.crn)

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
            withApplication(application)
            withPremises(premises)
            withBed(bedTwo)
            withArrivalDate(LocalDate.now().minusDays(48))
            withDepartureDate(LocalDate.now().minusDays(18))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          nonOverlappingBooking.cancellations += cancellationEntityFactory.produceAndPersist {
            withBooking(nonOverlappingBooking)
            withYieldedReason {
              cancellationReasonEntityFactory.produceAndPersist()
            }
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.post()
            .uri("cas3/bedspaces/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3BedspaceSearchParameters(
                startDate = LocalDate.now(),
                durationDays = 31,
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas3BedspaceSearchResults(
                  resultsRoomCount = 1,
                  resultsPremisesCount = 1,
                  resultsBedCount = 2,
                  results = listOf(
                    createBedspaceSearchResult(
                      premises,
                      roomOne,
                      bedOne,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      premises,
                      roomOne,
                      bedTwo,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
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
    }

    @Test
    fun `Searching for a bedspace in a Shared Property returns only bedspaces in shared properties`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val premises = createPremisesAndBedspacesWithCharacteristics(
          localAuthorityArea,
          searchPdu,
        )

        val expextedPremisesOne = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
        val expextedPremisesOneRoomOne = expextedPremisesOne.rooms.first()
        val expextedPremisesOneRoomTwo = expextedPremisesOne.rooms.drop(1).first()
        val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_MEN_ONLY_ID) }
        val expextedPremisesTwoRoomOne = expextedPremisesTwo.rooms.first()
        val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID) }
        val expextedPremisesThreeRoomOne = expextedPremisesThree.rooms.first()

        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(10),
              durationDays = 84,
              probationDeliveryUnits = listOf(searchPdu.id),
              premisesFilters = PremisesFilters(
                includedCharacteristicIds = listOf(getPremisesSharedPropertyCharacteristic()?.id!!),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 4,
                resultsPremisesCount = 3,
                resultsBedCount = 4,
                results = listOf(
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomTwo,
                    expextedPremisesOneRoomTwo.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                    roomCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesTwo,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      getSharedPropertyCharacteristicPair(),
                      getMenOnlyCharacteristicPair(),
                    ),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesThree,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      getSharedPropertyCharacteristicPair(),
                      getWomenOnlyCharacteristicPair(),
                    ),
                    roomCharacteristics = listOf(),
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
    fun `Bedspace search filter only returns included premises filters`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val characteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.premises)
        val characteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.premises)

        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicOne),
        )

        val premisesTwo = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Two",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicTwo),
        )

        createBedspace(premisesOne, "Room One", listOf())
        createBedspace(premisesTwo, "Room One", listOf())

        val result = getResponseForRequest(
          jwt,
          Cas3BedspaceSearchParameters(
            startDate = LocalDate.now().plusDays(21),
            durationDays = 84,
            probationDeliveryUnits = listOf(searchPdu.id),
            premisesFilters = PremisesFilters(
              includedCharacteristicIds = listOf(characteristicOne.id),
            ),
          ),
        )

        val returnedPremisesIds = result.results.map { it.premises.id }

        assertThat(returnedPremisesIds).containsExactly(premisesOne.id)
        assertThat(returnedPremisesIds).doesNotContain(premisesTwo.id)
      }
    }

    @Test
    fun `Bedspace search filter does not return excluded premises filters`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val characteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.premises)
        val characteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.premises)

        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicOne),
        )

        val premisesTwo = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Two",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicTwo),
        )

        createBedspace(premisesOne, "Room One", listOf())
        createBedspace(premisesTwo, "Room One", listOf())

        val result = getResponseForRequest(
          jwt,
          Cas3BedspaceSearchParameters(
            startDate = LocalDate.now().plusDays(17),
            durationDays = 84,
            probationDeliveryUnits = listOf(searchPdu.id),
            premisesFilters = PremisesFilters(
              excludedCharacteristicIds = listOf(characteristicOne.id),
            ),
          ),
        )

        val returnedPremisesIds = result.results.map { it.premises.id }

        assertThat(returnedPremisesIds).containsExactly(premisesTwo.id)
        assertThat(returnedPremisesIds).doesNotContain(premisesOne.id)
      }
    }

    @Test
    fun `Bedspace search filter only returns included bedspace filters`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val characteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.room)
        val characteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.room)

        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(),
        )

        val (_, premisesOneBedOne) = createBedspace(premisesOne, "Room One", listOf(characteristicOne))
        val (_, premisesOneBedTwo) = createBedspace(premisesOne, "Room One", listOf(characteristicTwo))

        val result = getResponseForRequest(
          jwt,
          Cas3BedspaceSearchParameters(
            startDate = LocalDate.now(),
            durationDays = 84,
            probationDeliveryUnits = listOf(searchPdu.id),
            bedspaceFilters = BedspaceFilters(
              includedCharacteristicIds = listOf(characteristicOne.id),
            ),
          ),
        )

        val returnedBedIds = result.results.map { it.bed.id }
        assertThat(returnedBedIds).containsExactly(premisesOneBedOne.id)
        assertThat(returnedBedIds).doesNotContain(premisesOneBedTwo.id)
      }
    }

    @Test
    fun `Bedspace search filter does not return excluded bedspace filters`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val characteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.room)
        val characteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.room)

        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicOne),
        )

        val (_, premisesOneBedOne) = createBedspace(premisesOne, "Room One", listOf(characteristicOne))
        val (_, premisesOneBedTwo) = createBedspace(premisesOne, "Room One", listOf(characteristicTwo))
        val (_, premisesOneBedThree) = createBedspace(premisesOne, "Room One", listOf(characteristicOne, characteristicTwo))

        val result = getResponseForRequest(
          jwt,
          Cas3BedspaceSearchParameters(
            startDate = LocalDate.now().minusDays(3),
            durationDays = 84,
            probationDeliveryUnits = listOf(searchPdu.id),
            bedspaceFilters = BedspaceFilters(
              excludedCharacteristicIds = listOf(characteristicOne.id),
            ),
          ),
        )

        val returnedBedIds = result.results.map { it.bed.id }
        assertThat(returnedBedIds).containsExactly(premisesOneBedTwo.id)
        assertThat(returnedBedIds).doesNotContain(premisesOneBedOne.id, premisesOneBedThree.id)
      }
    }

    @Test
    fun `Bedspace search filter returns correct results with multiple bedspace filters`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesCharacteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.premises)
        val premisesCharacteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.premises)
        val premisesCharacteristicThree =
          produceCharacteristic("CharacteristicThree", Characteristic.ModelScope.premises)

        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(premisesCharacteristicOne),
        )

        val premisesTwo = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Two",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(premisesCharacteristicOne, premisesCharacteristicTwo),
        )

        val premisesThree = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Three",
          probationRegion,
          localAuthorityArea,
          searchPdu,
          mutableListOf(premisesCharacteristicOne, premisesCharacteristicTwo, premisesCharacteristicThree),
        )

        val roomCharacteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.room)
        val roomCharacteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.room)
        val roomCharacteristicThree = produceCharacteristic("CharacteristicThree", Characteristic.ModelScope.room)

        // premises one beds
        val (_, premisesOneBedOne) = createBedspace(premisesOne, "11", listOf(roomCharacteristicOne))
        val (_, premisesOneBedTwo) = createBedspace(premisesOne, "12", listOf(roomCharacteristicOne, roomCharacteristicTwo))
        createBedspace(premisesOne, "13", listOf(roomCharacteristicOne, roomCharacteristicTwo, roomCharacteristicThree))

        // premises two beds
        val (_, premisesTwoBedOne) = createBedspace(premisesTwo, "21", listOf(roomCharacteristicOne))
        val (_, premisesTwoBedTwo) = createBedspace(premisesTwo, "22", listOf(roomCharacteristicOne, roomCharacteristicTwo))
        createBedspace(premisesTwo, "23", listOf(roomCharacteristicOne, roomCharacteristicTwo, roomCharacteristicThree))

        // premises three beds
        createBedspace(premisesThree, "31", listOf(roomCharacteristicOne))
        createBedspace(premisesThree, "32", listOf(roomCharacteristicOne, roomCharacteristicTwo))
        createBedspace(premisesThree, "33", listOf(roomCharacteristicOne, roomCharacteristicTwo, roomCharacteristicThree))

        val result = getResponseForRequest(
          jwt,
          Cas3BedspaceSearchParameters(
            startDate = LocalDate.now().plusDays(10),
            durationDays = 84,
            probationDeliveryUnits = listOf(searchPdu.id),
            bedspaceFilters = BedspaceFilters(
              includedCharacteristicIds = listOf(roomCharacteristicOne.id),
              excludedCharacteristicIds = listOf(roomCharacteristicThree.id),
            ),
            premisesFilters = PremisesFilters(
              includedCharacteristicIds = listOf(premisesCharacteristicOne.id),
              excludedCharacteristicIds = listOf(premisesCharacteristicThree.id),
            ),
          ),
        )

        // only premises one and two are returned, as both have PremisesCharacteristicOne and neither have PremisesCharacteristicThree
        val returnedPremisesIds = result.results.map { it.premises.id }
        assertThat(returnedPremisesIds).containsOnly(premisesOne.id, premisesTwo.id)
        assertThat(returnedPremisesIds).doesNotContain(premisesThree.id)

        // only rooms 1 and 2 should be returned, as both have RoomCharacteristicOne and neither have RoomCharacteristicThree
        val returnedBedIds = result.results.map { it.bed.id }
        assertThat(returnedBedIds).hasSize(4)
          .containsExactlyInAnyOrder(
            premisesOneBedOne.id,
            premisesOneBedTwo.id,
            premisesTwoBedOne.id,
            premisesTwoBedTwo.id,
          )
      }
    }

    @Test
    fun `Searching for a bedspace in a Single Occupancy Property returns only bedspaces in properties with single occupancy`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val premises = createPremisesAndBedspacesWithCharacteristics(
          localAuthorityArea,
          searchPdu,
        )

        val expextedPremisesOne = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID) }
        val expextedPremisesOneRoomOne = expextedPremisesOne.rooms.first()
        val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID) }
        val expextedPremisesTwoRoomOne = expextedPremisesTwo.rooms.first()
        val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
        val expextedPremisesThreeRoomOne = expextedPremisesThree.rooms.first()
        val expextedPremisesFour = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID) }
        val expextedPremisesFourRoomOne = expextedPremisesFour.rooms.first()

        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(2),
              durationDays = 84,
              probationDeliveryUnits = listOf(searchPdu.id),
              premisesFilters = PremisesFilters(
                includedCharacteristicIds = listOf(getPremisesSingleOccupancyCharacteristic()?.id!!),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 4,
                resultsPremisesCount = 4,
                resultsBedCount = 4,
                results = listOf(
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesTwo,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      getSingleOccupancyCharacteristicPair(),
                      getMenOnlyCharacteristicPair(),
                    ),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesThree,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                    roomCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesFour,
                    expextedPremisesFourRoomOne,
                    expextedPremisesFourRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      getWomenOnlyCharacteristicPair(),
                      getSingleOccupancyCharacteristicPair(),
                    ),
                    roomCharacteristics = listOf(),
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
    fun `Searching for a bedspace with wheelchair accessible returns only bedspaces with wheelchair accessible`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val premises = createPremisesAndBedspacesWithCharacteristics(
          localAuthorityArea,
          searchPdu,
        )

        val expextedPremisesOne = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
        val expextedPremisesOneRoomOne = expextedPremisesOne.rooms.drop(1).first()
        val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
        val expextedPremisesTwoRoomOne = expextedPremisesTwo.rooms.first()

        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(14),
              durationDays = 84,
              probationDeliveryUnits = listOf(searchPdu.id),
              bedspaceFilters = BedspaceFilters(
                includedCharacteristicIds = listOf(getWheelchairAccessibleCharacteristic()?.id!!),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 2,
                resultsPremisesCount = 2,
                resultsBedCount = 2,
                results = listOf(
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                    roomCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesTwo,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoRoomOne.beds.first(),
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                    roomCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
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
    fun `Searching for men only bedspaces returns bedspaces suitable for men`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(3).toList()

          val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          }

          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val premises = createPremisesAndBedspacesWithCharacteristics(
            localAuthorityArea,
            searchPdu,
          )

          val expextedPremisesOne = premises.first { p -> p.id == UUID.fromString(PREMISES_MEN_ONLY_ID) }
          val expextedPremisesOneRoomOne = expextedPremisesOne.rooms.first()
          val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
          val expextedPremisesTwoRoomOne = expextedPremisesTwo.rooms.first()
          val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_MEN_ONLY_ID) }
          val expextedPremisesThreeRoomOne = expextedPremisesThree.rooms.first()
          val expextedPremisesFour = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID) }
          val expextedPremisesFourRoomOne = expextedPremisesFour.rooms.first()
          val expextedPremisesFive = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID) }
          val expextedPremisesFiveRoomOne = expextedPremisesFive.rooms.first()

          val offenderDetailsOne = offenders.first().first
          CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsOne)
            .withGender("Male")
            .produce()

          val premisesPubNearBy = premises.first { p -> p.id == UUID.fromString(PREMISES_PUB_NEARBY_ID) }
          val premisesPubNearByRoomOne = premisesPubNearBy.rooms.first()
          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesPubNearBy)
            withBed(premisesPubNearByRoomOne.beds.first())
            withArrivalDate(LocalDate.now().minusDays(20))
            withDepartureDate(LocalDate.now().plusDays(71))
            withCrn(offenderDetailsOne.otherIds.crn)
          }

          val offenderDetailsTwo = offenders.drop(1).first().first
          val caseSummaryTwo = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsTwo)
            .withGender("Male")
            .produce()

          val premisesSharedProperty = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
          val premisesSharedPropertyRoomTwo = premisesSharedProperty.rooms.drop(1).first()
          val bookingOffenderTwo = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSharedPropertyRoomTwo.premises)
            withBed(premisesSharedPropertyRoomTwo.beds.first())
            withArrivalDate(LocalDate.now().minusDays(6))
            withDepartureDate(LocalDate.now().plusDays(23))
            withCrn(offenderDetailsTwo.otherIds.crn)
          }

          val offenderDetailsThree = offenders.drop(2).first().first
          CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsThree)
            .withGender("Female")
            .produce()

          val premisesSingleOccupancyWheelchairAccessible = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
          val premisesSingleOccupancyWheelchairAccessibleRoomOne = premisesSingleOccupancyWheelchairAccessible.rooms.first()
          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSingleOccupancyWheelchairAccessible)
            withBed(premisesSingleOccupancyWheelchairAccessibleRoomOne.beds.first())
            withArrivalDate(LocalDate.now().minusDays(28))
            withDepartureDate(LocalDate.now().plusDays(34))
            withCrn(offenderDetailsThree.otherIds.crn)
          }

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseSummaryTwo))

          webTestClient.post()
            .uri("cas3/bedspaces/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3BedspaceSearchParameters(
                startDate = LocalDate.now().plusDays(4),
                durationDays = 84,
                probationDeliveryUnits = listOf(searchPdu.id),
                premisesFilters = PremisesFilters(
                  includedCharacteristicIds = listOf(getPremisesMenOnlyCharacteristic()?.id!!),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas3BedspaceSearchResults(
                  resultsRoomCount = 5,
                  resultsPremisesCount = 5,
                  resultsBedCount = 5,
                  results = listOf(
                    createBedspaceSearchResult(
                      expextedPremisesOne,
                      expextedPremisesOneRoomOne,
                      expextedPremisesOneRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getMenOnlyCharacteristicPair()),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesTwo,
                      expextedPremisesTwoRoomOne,
                      expextedPremisesTwoRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(
                        Cas3BedspaceSearchResultOverlap(
                          name = "${offenderDetailsTwo.firstName} ${offenderDetailsTwo.surname}",
                          crn = offenderDetailsTwo.otherIds.crn,
                          personType = PersonType.fullPerson,
                          days = 20,
                          bookingId = bookingOffenderTwo.id,
                          roomId = premisesSharedPropertyRoomTwo.id,
                          isSexualRisk = false,
                          sex = "Male",
                          assessmentId = null,
                        ),
                      ),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesThree,
                      expextedPremisesThreeRoomOne,
                      expextedPremisesThreeRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getMenOnlyCharacteristicPair(),
                        getSharedPropertyCharacteristicPair(),
                      ),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFour,
                      expextedPremisesFourRoomOne,
                      expextedPremisesFourRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFive,
                      expextedPremisesFiveRoomOne,
                      expextedPremisesFiveRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getSingleOccupancyCharacteristicPair(),
                        getMenOnlyCharacteristicPair(),
                      ),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                  ),
                ),
              ),
              true,
            )
        }
      }
    }

    @Test
    fun `Searching for women only bedspaces returns bedspaces suitable for women`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(3).toList()

          val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          }

          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val premises = createPremisesAndBedspacesWithCharacteristics(
            localAuthorityArea,
            searchPdu,
          )

          val expextedPremisesOne = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
          val expextedPremisesOneRoomOne = expextedPremisesOne.rooms.first()
          val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID) }
          val expextedPremisesTwoRoomOne = expextedPremisesTwo.rooms.first()
          val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
          val expextedPremisesThreeRoomOne = expextedPremisesThree.rooms.first()
          val expextedPremisesFour = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID) }
          val expextedPremisesFourRoomOne = expextedPremisesFour.rooms.first()
          val expextedPremisesFive = premises.first { p -> p.id == UUID.fromString(PREMISES_WOMEN_ONLY_ID) }
          val expextedPremisesFiveRoomOne = expextedPremisesFive.rooms.first()

          val offenderDetailsOne = offenders.first().first
          CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsOne)
            .withGender("Male")
            .produce()

          val premisesSingleOccupancy = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID) }
          val premisesSingleOccupancyRoomOne = premisesSingleOccupancy.rooms.first()
          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSingleOccupancy)
            withBed(premisesSingleOccupancyRoomOne.beds.first())
            withArrivalDate(LocalDate.now().minusDays(10))
            withDepartureDate(LocalDate.now().plusDays(33))
            withCrn(offenderDetailsOne.otherIds.crn)
          }

          val offenderDetailsTwo = offenders.drop(1).first().first
          val caseSummaryTwo = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsTwo)
            .withGender("Female")
            .produce()

          val premisesSharedProperty = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
          val premisesSharedPropertyRoomTwo = premisesSharedProperty.rooms.drop(1).first()
          val bookingOffenderTwo = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSharedProperty)
            withBed(premisesSharedPropertyRoomTwo.beds.first())
            withArrivalDate(LocalDate.now().minusDays(2))
            withDepartureDate(LocalDate.now().plusDays(65))
            withCrn(offenderDetailsTwo.otherIds.crn)
          }

          val offenderDetailsThree = offenders.drop(2).first().first
          CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsThree)
            .withGender("Female")
            .produce()

          val premisesPubNearby = premises.first { p -> p.id == UUID.fromString(PREMISES_PUB_NEARBY_ID) }
          val premisesPubNearbyRoomOne = premisesPubNearby.rooms.first()
          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesPubNearby)
            withBed(premisesPubNearbyRoomOne.beds.first())
            withArrivalDate(LocalDate.now().minusDays(3))
            withDepartureDate(LocalDate.now().plusDays(102))
            withCrn(offenderDetailsThree.otherIds.crn)
          }

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseSummaryTwo))

          webTestClient.post()
            .uri("cas3/bedspaces/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3BedspaceSearchParameters(
                startDate = LocalDate.now(),
                durationDays = 84,
                probationDeliveryUnits = listOf(searchPdu.id),
                premisesFilters = PremisesFilters(
                  includedCharacteristicIds = listOf(getPremisesWomenOnlyCharacteristic()?.id!!),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas3BedspaceSearchResults(
                  resultsRoomCount = 5,
                  resultsPremisesCount = 5,
                  resultsBedCount = 5,
                  results = listOf(
                    createBedspaceSearchResult(
                      expextedPremisesOne,
                      expextedPremisesOneRoomOne,
                      expextedPremisesOneRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(
                        Cas3BedspaceSearchResultOverlap(
                          name = "${offenderDetailsTwo.firstName} ${offenderDetailsTwo.surname}",
                          crn = offenderDetailsTwo.otherIds.crn,
                          personType = PersonType.fullPerson,
                          days = 66,
                          bookingId = bookingOffenderTwo.id,
                          roomId = premisesSharedPropertyRoomTwo.id,
                          isSexualRisk = false,
                          sex = "Female",
                          assessmentId = null,
                        ),
                      ),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesTwo,
                      expextedPremisesTwoRoomOne,
                      expextedPremisesTwoRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getWomenOnlyCharacteristicPair(),
                        getSharedPropertyCharacteristicPair(),
                      ),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesThree,
                      expextedPremisesThreeRoomOne,
                      expextedPremisesThreeRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                      roomCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFour,
                      expextedPremisesFourRoomOne,
                      expextedPremisesFourRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getSingleOccupancyCharacteristicPair(),
                        getWomenOnlyCharacteristicPair(),
                      ),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFive,
                      expextedPremisesFiveRoomOne,
                      expextedPremisesFiveRoomOne.beds.first(),
                      searchPdu.name,
                      numberOfBeds = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getWomenOnlyCharacteristicPair()),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                  ),
                ),
              ),
              true,
            )
        }
      }
    }

    @Test
    fun `Searching for a bedspace should not return bedspace when given premises bedspace endDate is same as search start date`() {
      givenAUser { _, jwt ->
        val durationDays = 7L
        val searchStartDate = LocalDate.parse("2023-03-23")
        val searchPdu = createPremisesWithBedspaceEndDate(searchStartDate, searchStartDate)

        searchCas3BedspaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a bedspace should not return bedspace when given premises bedspace endDate is between search start date and end date`() {
      givenAUser { _, jwt ->
        val durationDays = 7L
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(2)
        val searchPdu = createPremisesWithBedspaceEndDate(searchStartDate, bedEndDate)

        searchCas3BedspaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a bedspace should not return bed when given premises bedspace endDate is same as search end date`() {
      givenAUser { _, jwt ->
        val durationDays = 7L
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(durationDays.toLong() - 1)
        val searchPdu = createPremisesWithBedspaceEndDate(searchStartDate, bedEndDate)

        searchCas3BedspaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a bedspace should not return bed when given premises bedspace endDate less than than search start date`() {
      givenAUser { _, jwt ->
        val durationDays = 7L
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.minusDays(1)
        val searchPdu = createPremisesWithBedspaceEndDate(searchStartDate, bedEndDate)

        searchCas3BedspaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a bedspace should return single bed when given premises has got 2 rooms where one with endDate and another room without end date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.now().plusDays(3)
        val durationDays = 7L
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
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    roomWithoutEndDate,
                    bedWithoutEndDate,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace should return bed when given premises bedspace endDate after search end date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val durationDays = 7L
        val searchStartDate = LocalDate.now().plusDays(3)
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
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    room,
                    bed,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace should return no bed when given premises has got 2 rooms where one with endDate in the passed and another room with matching end date`() {
      givenAUser { _, jwt ->
        val durationDays = 7L
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(1)
        val searchPdu = createPremisesWithBedspaceEndDate(searchStartDate, bedEndDate)

        searchCas3BedspaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a bedspace should return bed matches searching pdu`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withName(randomStringLowerCase(8))
        withProbationRegion(probationRegion)
      }

      val pduTwo = probationDeliveryUnitFactory.produceAndPersist {
        withName(randomStringLowerCase(8))
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.now().plusDays(3)
        val durationDays = 7L
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
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 2,
                resultsPremisesCount = 1,
                resultsBedCount = 2,
                results = listOf(
                  createBedspaceSearchResult(
                    premisesOne,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premisesOne,
                    roomTwo,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
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
    fun `Searching for a bedspace in multiple pdus should return bedspace matches searching pdus`() {
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

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val durationDays = 7L
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduOne)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesOne, bedOnePremisesOne) = createBedspace(premisesOne, "Room One", listOf())
        val (roomTwoPremisesOne, bedTwoPremisesOne) = createBedspace(premisesOne, "Room Two", listOf())

        val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        createBedspace(premisesTwo, "Room One", listOf())

        val premisesThree = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Three")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduThree)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesThree, bedOnePremisesThree) = createBedspace(premisesThree, "Room One", listOf())

        webTestClient.post()
          .uri("cas3/bedspaces/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = LocalDate.now().plusDays(5),
              durationDays = durationDays,
              probationDeliveryUnits = listOf(pduOne.id, pduThree.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas3BedspaceSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 2,
                resultsBedCount = 3,
                results = listOf(
                  createBedspaceSearchResult(
                    premisesOne,
                    roomOnePremisesOne,
                    bedOnePremisesOne,
                    pduOne.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premisesOne,
                    roomTwoPremisesOne,
                    bedTwoPremisesOne,
                    pduOne.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premisesThree,
                    roomOnePremisesThree,
                    bedOnePremisesThree,
                    pduThree.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    private fun searchCas3BedspaceAndAssertNoAvailability(
      jwt: String,
      searchStartDate: LocalDate,
      durationDays: Long,
      pduId: UUID,
    ) {
      webTestClient.post()
        .uri("cas3/bedspaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas3BedspaceSearchParameters(
            startDate = searchStartDate,
            durationDays = durationDays,
            probationDeliveryUnits = listOf(pduId),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            Cas3BedspaceSearchResults(
              resultsRoomCount = 0,
              resultsPremisesCount = 0,
              resultsBedCount = 0,
              results = listOf(),
            ),
          ),
        )
    }

    @SuppressWarnings("LongParameterList")
    private fun createPremisesWithCharacteristics(
      premisesId: UUID,
      premisesName: String,
      probationRegion: ProbationRegionEntity,
      localAuthorityArea: LocalAuthorityAreaEntity,
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      characteristics: MutableList<CharacteristicEntity>,
    ): TemporaryAccommodationPremisesEntity = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withId(premisesId)
      withName(premisesName)
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withStatus(PropertyStatus.active)
      withCharacteristics(characteristics)
    }

    private fun createBedspace(premises: PremisesEntity, roomName: String, roomCharacteristics: List<CharacteristicEntity>): Pair<RoomEntity, BedEntity> {
      val room = when {
        roomCharacteristics.isEmpty() -> {
          roomEntityFactory.produceAndPersist {
            withName(roomName)
            withPremises(premises)
          }
        }
        else -> {
          roomEntityFactory.produceAndPersist {
            withName(roomName)
            withPremises(premises)
            withCharacteristicsList(roomCharacteristics)
          }
        }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName(randomStringMultiCaseWithNumbers(10))
        withRoom(room)
      }

      room.beds.add(bed)
      premises.rooms.add(room)

      return Pair(room, bed)
    }

    private fun createPremisesWithBedspaceEndDate(
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
    private fun createBedspaceSearchResult(
      premises: PremisesEntity,
      room: RoomEntity,
      bed: BedEntity,
      pduName: String,
      numberOfBeds: Int,
      numberOfBookedBeds: Int,
      premisesCharacteristics: List<CharacteristicPair>,
      roomCharacteristics: List<CharacteristicPair>,
      overlaps: List<Cas3BedspaceSearchResultOverlap>,
    ): Cas3BedspaceSearchResult = Cas3BedspaceSearchResult(
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
        characteristics = roomCharacteristics,
      ),
      bed = BedSearchResultBedSummary(
        id = bed.id,
        name = bed.name,
      ),
      overlaps = overlaps,
    )

    private fun createPremisesAndBedspacesWithCharacteristics(
      localAuthorityArea: LocalAuthorityAreaEntity,
      pdu: ProbationDeliveryUnitEntity,
    ): List<PremisesEntity> {
      val premisesSingleOccupancyCharacteristic = getPremisesSingleOccupancyCharacteristic()
      val premisesSharedPropertyCharacteristic = getPremisesSharedPropertyCharacteristic()
      val premisesMenOnlyCharacteristic = getPremisesMenOnlyCharacteristic()
      val premisesWomenOnlyCharacteristic = getPremisesWomenOnlyCharacteristic()
      val premisesPubNearbyCharacteristic = getPremisesPubNearByCharacteristic()
      val wheelchairAccessibleCharacteristic = getWheelchairAccessibleCharacteristic()

      val premisesSingleOccupancy = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID),
        "Premises Single Occupancy",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic!!),
      )
      createBedspace(premisesSingleOccupancy, "Premises Single Occupancy Room One", listOf())

      val premisesSingleOccupancyWithWheelchairAccessible = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID),
        "Premises Single Occupancy with Wheelchair Accessible",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic!!),
      )
      createBedspace(
        premisesSingleOccupancyWithWheelchairAccessible,
        "Premises Single Occupancy with Wheelchair Accessible Room One",
        listOf(wheelchairAccessibleCharacteristic!!),
      )

      val premisesSharedProperty = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SHARED_PROPERTY_ID),
        "Premises Shared Property",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSharedPropertyCharacteristic!!),
      )

      createBedspace(premisesSharedProperty, "Premises Shared Property Room One", listOf())
      createBedspace(
        premisesSharedProperty,
        "Premises Shared Property with Wheelchair Accessible Room One",
        listOf(wheelchairAccessibleCharacteristic),
      )

      val premisesMenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_MEN_ONLY_ID),
        "Premises Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic!!),
      )
      createBedspace(premisesMenOnly, "Premises Men Only Room One", listOf())

      val premisesWomenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_WOMEN_ONLY_ID),
        "Premises Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic!!),
      )
      createBedspace(premisesWomenOnly, "Premises Women Only Room One", listOf())

      val premisesPubNearby = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_PUB_NEARBY_ID),
        "Premises Pub Nearby",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesPubNearbyCharacteristic!!),
      )
      createBedspace(premisesPubNearby, "Premises Pub Nearby Room One", listOf())

      val premisesSingleOccupancyMenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID),
        "Premises Single Occupancy - Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic, premisesSingleOccupancyCharacteristic),
      )
      createBedspace(
        premisesSingleOccupancyMenOnly,
        "Premises Single Occupancy Men Only Room One",
        listOf(),
      )

      val premisesSingleOccupancyWomenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID),
        "Premises Single Occupancy - Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic, premisesSingleOccupancyCharacteristic),
      )
      createBedspace(
        premisesSingleOccupancyWomenOnly,
        "Premises Single Occupancy Women Only Room One",
        listOf(),
      )

      val premisesSharedPropertyMenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SHARED_PROPERTY_MEN_ONLY_ID),
        "Premises Shared Property - Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic, premisesSharedPropertyCharacteristic),
      )
      createBedspace(
        premisesSharedPropertyMenOnly,
        "Premises Shared Property Men Only Room One",
        listOf(),
      )

      val premisesSharedPropertyWomenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID),
        "Premises Shared Property - Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic, premisesSharedPropertyCharacteristic),
      )
      createBedspace(
        premisesSharedPropertyWomenOnly,
        "Premises Shared Property Women Only Room One",
        listOf(),
      )

      return listOf(
        premisesSingleOccupancy,
        premisesSingleOccupancyMenOnly,
        premisesSingleOccupancyWomenOnly,
        premisesSingleOccupancyWithWheelchairAccessible,
        premisesSharedProperty,
        premisesSharedPropertyMenOnly,
        premisesSharedPropertyWomenOnly,
        premisesMenOnly,
        premisesWomenOnly,
        premisesPubNearby,
      )
    }

    private fun produceCharacteristic(
      propertyName: String,
      modelScope: Characteristic.ModelScope,
    ): CharacteristicEntity {
      val characteristicTwo = characteristicRepository.save(
        CharacteristicEntityFactory().withPropertyName(propertyName)
          .withServiceScope(ServiceName.temporaryAccommodation.value)
          .withModelScope(modelScope.value).produce(),
      )
      return characteristicTwo
    }

    private fun getResponseForRequest(jwt: String, searchParameters: Cas3BedspaceSearchParameters) = webTestClient.post()
      .uri("cas3/bedspaces/search")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(searchParameters)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(Cas3BedspaceSearchResults::class.java)
      .returnResult()
      .responseBody!!

    private fun createAssessment(user: UserEntity, crn: String, sexualRisk: Boolean? = null): Pair<TemporaryAccommodationApplicationEntity, AssessmentEntity> {
      val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
        withCrn(crn)
        withCreatedByUser(user)
        withProbationRegion(user.probationRegion)
        if (sexualRisk != null) {
          withHasHistoryOfSexualOffence(sexualRisk)
          withIsConcerningSexualBehaviour(sexualRisk)
          withHasRegisteredSexOffender(sexualRisk)
        }
      }

      val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
        withApplication(application)
      }

      return Pair(application, assessment)
    }

    private fun getPremisesSingleOccupancyCharacteristic(): CharacteristicEntity? = characteristicRepository.findByPropertyName(CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY, ServiceName.temporaryAccommodation.value)
    private fun getPremisesSharedPropertyCharacteristic(): CharacteristicEntity? = characteristicRepository.findByPropertyName(CAS3_PROPERTY_NAME_SHARED_PROPERTY, ServiceName.temporaryAccommodation.value)
    private fun getPremisesMenOnlyCharacteristic(): CharacteristicEntity? = characteristicRepository.findByPropertyName(CAS3_PROPERTY_NAME_MEN_ONLY, ServiceName.temporaryAccommodation.value)
    private fun getPremisesWomenOnlyCharacteristic(): CharacteristicEntity? = characteristicRepository.findByPropertyName(CAS3_PROPERTY_NAME_WOMEN_ONLY, ServiceName.temporaryAccommodation.value)
    private fun getWheelchairAccessibleCharacteristic(): CharacteristicEntity? = characteristicRepository.findByPropertyName(CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE, ServiceName.temporaryAccommodation.value)
    private fun getPremisesPubNearByCharacteristic(): CharacteristicEntity? = characteristicRepository.findByPropertyName(CAS3_PROPERTY_NAME_PUB_NEAR_BY, ServiceName.temporaryAccommodation.value)

    private fun getSharedPropertyCharacteristicPair() = CharacteristicPair(propertyName = CAS3_PROPERTY_NAME_SHARED_PROPERTY, name = "Shared property")
    private fun getSingleOccupancyCharacteristicPair() = CharacteristicPair(propertyName = CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY, name = "Single occupancy")
    private fun getMenOnlyCharacteristicPair() = CharacteristicPair(propertyName = CAS3_PROPERTY_NAME_MEN_ONLY, name = "Men only")
    private fun getWomenOnlyCharacteristicPair() = CharacteristicPair(propertyName = CAS3_PROPERTY_NAME_WOMEN_ONLY, name = "Women only")
    private fun getWheelchairAccessibleCharacteristicPair() = CharacteristicPair(propertyName = CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE, name = "Wheelchair accessible")
  }
}
