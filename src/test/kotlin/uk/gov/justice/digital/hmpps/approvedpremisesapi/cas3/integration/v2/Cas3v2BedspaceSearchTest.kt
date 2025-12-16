package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository.Constants.CAS3_PROPERTY_NAME_MEN_ONLY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository.Constants.CAS3_PROPERTY_NAME_PUB_NEAR_BY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository.Constants.CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository.Constants.CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository.Constants.CAS3_PROPERTY_NAME_WOMEN_ONLY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSearchResultBedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.BedspaceFilters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.PremisesFilters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3PremisesAndBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_SHARED_PROPERTY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

@SuppressWarnings("LargeClass")
class Cas3v2BedspaceSearchTest : IntegrationTestBase() {
  @Autowired
  private lateinit var migrationJobService: MigrationJobService

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
        .uri("/cas3/v2/bedspaces/search")
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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = LocalDate.now().minusDays(30), endDate = null)
        val searchPdu = premises.probationDeliveryUnit
        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 1,
                resultsBedspaceCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    bedspace,
                    pduName = searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace returns an upcoming bedspace in the schedule to unarchive premises when the bedspace is online within the search range`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = cas3PremisesEntityFactory
          .produceAndPersist {
            withStatus(Cas3PremisesStatus.online)
            withStartDate(LocalDate.now().minusDays(90))
            withEndDate(null)
            withProbationDeliveryUnit(
              probationDeliveryUnitFactory.produceAndPersist {
                withProbationRegion(user.probationRegion)
              },
            )
            withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
          }
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withStartDate(LocalDate.now().plusDays(3))
          withEndDate(null)
        }
        val searchPdu = premises.probationDeliveryUnit
        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 1,
                resultsBedspaceCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    bedspace,
                    pduName = searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = cas3PremisesEntityFactory
          .produceAndPersist {
            withStatus(Cas3PremisesStatus.online)
            withStartDate(LocalDate.now().minusDays(90))
            withEndDate(null)
            withProbationDeliveryUnit(
              probationDeliveryUnitFactory.produceAndPersist {
                withProbationRegion(user.probationRegion)
              },
            )
            withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
          }
        cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withStartDate(LocalDate.now().plusDays(3))
          withEndDate(null)
        }
        val searchPdu = premises.probationDeliveryUnit!!
        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsBedspaceCount = 0,
                resultsPremisesCount = 0,
                results = listOf(),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace returns results that do not include bedspaces with current turnarounds`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        }
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val premises = cas3PremisesEntityFactory.produceAndPersist {
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withStatus(Cas3PremisesStatus.online)
        }
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("Matching Bed")
        }
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBedspace(bedspace)
          withArrivalDate(LocalDate.parse("2022-12-21"))
          withDepartureDate(LocalDate.parse("2023-03-21"))
        }
        val turnaround = cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(2)
        }
        booking.turnarounds = mutableListOf(turnaround)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        searchCas3BedspaceAndAssertNoAvailability(
          jwt,
          searchStartDate = LocalDate.parse("2023-03-23"),
          durationDays = 7,
          pduId = searchPdu.id,
        )
      }
    }

    @Test
    fun `Searching for a bedspace returns results that do not include bedspaces with voids`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        val startDate = LocalDate.now().minusDays(30)
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate, endDate = null)
        val voidBedspaceReason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withStartDate(startDate)
          withEndDate(startDate.plusDays(10))
          withBedspace(bedspace)
          withYieldedReason { voidBedspaceReason }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val searchPdu = premises.probationDeliveryUnit
        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3BedspaceSearchParameters(
              startDate = startDate,
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 0,
                resultsBedspaceCount = 0,
                results = listOf(),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace returns results when existing booking departure date is same as search start date`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        }
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val premises = cas3PremisesEntityFactory.produceAndPersist {
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withStatus(Cas3PremisesStatus.online)
        }
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("Matching Bedspace")
        }
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBedspace(bedspace)
          withArrivalDate(LocalDate.parse("2022-12-21"))
          withDepartureDate(LocalDate.parse("2023-03-21"))
        }
        val turnaround = cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(2)
        }
        booking.turnarounds = mutableListOf(turnaround)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        searchCas3BedspaceAndAssertNoAvailability(
          jwt,
          searchStartDate = LocalDate.parse("2023-03-21"),
          durationDays = 7,
          pduId = searchPdu.id,
        )
      }
    }

    @ParameterizedTest
    @CsvSource("true,cas3/v2/bedspaces/search", "false,cas3/v2/bedspaces/search")
    fun `Searching for a bedspace returns results which include overlapping bookings for bedspaces in the same premises`(sexualRisk: Boolean, baseUrl: String) {
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
          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withStatus(Cas3PremisesStatus.online)
          }
          val bedspaceOne = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("matching bedspace")
            withPremises(premises)
          }
          val bedspaceTwo = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("matching bedspace, but with an overlapping booking")
            withPremises(premises)
          }
          val bedspaceThree = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("another bedspace with an overlapping booking")
            withPremises(premises)
          }
          val bedspaceFour = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("yet another bedspace with an overlapping booking")
            withPremises(premises)
          }

          val fullPersonOffenderDetails = offenders.first().first
          val fullPersonApplication = applications.first()
          val fullPersonAssessment = assessments.first()
          val fullPersonCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(fullPersonOffenderDetails)
            .withPnc(fullPersonOffenderDetails.otherIds.pncNumber)
            .produce()

          val overlappingBookingSameBedspaces = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(fullPersonApplication)
            withPremises(premises)
            withBedspace(bedspaceTwo)
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

          val currentRestrictionOverlappingBooking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(currentRestrictionApplication)
            withPremises(premises)
            withBedspace(bedspaceThree)
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

          val userExcludedOverlappingBooking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(userExcludedApplication)
            withPremises(premises)
            withBedspace(bedspaceFour)
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
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
                Cas3v2BedspaceSearchResults(
                  resultsPremisesCount = 1,
                  resultsBedspaceCount = 1,
                  results = listOf(
                    createBedspaceSearchResult(
                      premises,
                      bedspaceOne,
                      pduName = searchPdu.name,
                      numberOfBedspaces = 4,
                      numberOfBookedBeds = 3,
                      premisesCharacteristics = listOf(),
                      bedspaceCharacteristics = listOf(),
                      listOf(
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "${fullPersonCaseSummary.name.forename} ${fullPersonCaseSummary.name.surname}",
                          crn = fullPersonCaseSummary.crn,
                          personType = PersonType.fullPerson,
                          sex = fullPersonCaseSummary.gender!!,
                          days = 15,
                          bookingId = overlappingBookingSameBedspaces.id,
                          bedspaceId = bedspaceTwo.id,
                          assessmentId = fullPersonAssessment.id,
                          isSexualRisk = sexualRisk,
                        ),
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "Limited Access Offender",
                          crn = currentRestrictionCaseSummary.crn,
                          personType = PersonType.restrictedPerson,
                          days = 5,
                          bookingId = currentRestrictionOverlappingBooking.id,
                          bedspaceId = bedspaceThree.id,
                          assessmentId = currentRestrictionAssessment.id,
                          isSexualRisk = sexualRisk,
                        ),
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "Limited Access Offender",
                          crn = userExcludedCaseSummary.crn,
                          personType = PersonType.restrictedPerson,
                          days = 7,
                          bookingId = userExcludedOverlappingBooking.id,
                          bedspaceId = bedspaceFour.id,
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
          val premisesOne = cas3PremisesEntityFactory.produceAndPersist {
            withName("Premises One")
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withStatus(Cas3PremisesStatus.online)
          }
          val premisesTwo = cas3PremisesEntityFactory.produceAndPersist {
            withName("Premises Two")
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withStatus(Cas3PremisesStatus.online)
          }
          val matchingBedInPremisesOne = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premisesOne)
            withReference("matching bed in premises one")
          }
          val overlappingBedInPremisesOne = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premisesOne)
            withReference("overlapping bed in premises one")
          }
          val matchingBedInPremisesTwo = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premisesTwo)
            withReference("matching bed in premises two")
          }
          val overlappingBedInPremisesTwo = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premisesTwo)
            withReference("overlapping bed in premises two")
          }
          val overlappingBookingForBedInPremisesOne = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesOne)
            withBedspace(overlappingBedInPremisesOne)
            withArrivalDate(LocalDate.now().minusDays(19))
            withDepartureDate(LocalDate.now().plusDays(16))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }
          val overlappingBookingForBedInPremisesTwo = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesTwo)
            withBedspace(overlappingBedInPremisesTwo)
            withArrivalDate(LocalDate.now().plusDays(26))
            withDepartureDate(LocalDate.now().plusDays(57))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          val cancelledOverlappingBookingForBedInPremisesTwo = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesTwo)
            withBedspace(overlappingBedInPremisesTwo)
            withArrivalDate(LocalDate.now().minusDays(9))
            withDepartureDate(LocalDate.now().plusDays(7))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          cas3CancellationEntityFactory.produceAndPersist {
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
            .uri("cas3/v2/bedspaces/search")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
                Cas3v2BedspaceSearchResults(
                  resultsPremisesCount = 2,
                  resultsBedspaceCount = 2,
                  results = listOf(
                    createBedspaceSearchResult(
                      premisesOne,
                      bedspace = matchingBedInPremisesOne,
                      pduName = searchPdu.name,
                      numberOfBedspaces = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(),
                      bedspaceCharacteristics = listOf(),
                      listOf(
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
                          crn = overlappingBookingForBedInPremisesOne.crn,
                          personType = PersonType.fullPerson,
                          sex = caseSummary.gender!!,
                          days = 15,
                          bookingId = overlappingBookingForBedInPremisesOne.id,
                          bedspaceId = overlappingBedInPremisesOne.id,
                          assessmentId = assessment.id,
                          isSexualRisk = false,
                        ),
                      ),
                    ),
                    createBedspaceSearchResult(
                      premisesTwo,
                      bedspace = matchingBedInPremisesTwo,
                      pduName = searchPdu.name,
                      numberOfBedspaces = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
                          crn = overlappingBookingForBedInPremisesTwo.crn,
                          personType = PersonType.fullPerson,
                          sex = caseSummary.gender!!,
                          days = 7,
                          bookingId = overlappingBookingForBedInPremisesTwo.id,
                          bedspaceId = overlappingBedInPremisesTwo.id,
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
        val premises = cas3PremisesEntityFactory.produceAndPersist {
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withStatus(Cas3PremisesStatus.online)
        }
        val bedOne = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("1 - matching bed with no bookings")
        }
        val bedTwo = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("2 - matching bed with non-overlapping booking")
        }
        val nonOverlappingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBedspace(bedTwo)
          withArrivalDate(LocalDate.now().plusDays(60))
          withDepartureDate(LocalDate.now().plusDays(90))
          withCrn(randomStringMultiCaseWithNumbers(16))
        }

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 1,
                resultsBedspaceCount = 2,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    bedOne,
                    pduName = searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premises,
                    bedTwo,
                    pduName = searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
          .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
          .jsonPath("$.results[*].overlaps[*].bedspaceId").value(Matchers.not(nonOverlappingBooking.bedspace.id))
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
          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withStatus(Cas3PremisesStatus.online)
          }
          val bedOne = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withReference("1 - matching bed with no bookings")
          }
          val bedTwo = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withReference("2 - matching bed with a cancelled booking")
          }
          val nonOverlappingBooking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premises)
            withBedspace(bedTwo)
            withArrivalDate(LocalDate.now().minusDays(48))
            withDepartureDate(LocalDate.now().minusDays(18))
            withCrn(offenderDetails.otherIds.crn)
          }

          nonOverlappingBooking.cancellations += cas3CancellationEntityFactory.produceAndPersist {
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
            .uri("cas3/v2/bedspaces/search")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
                Cas3v2BedspaceSearchResults(
                  resultsPremisesCount = 1,
                  resultsBedspaceCount = 2,
                  results = listOf(
                    createBedspaceSearchResult(
                      premises,
                      bedOne,
                      pduName = searchPdu.name,
                      numberOfBedspaces = 2,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      premises,
                      bedTwo,
                      pduName = searchPdu.name,
                      numberOfBedspaces = 2,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                  ),
                ),
              ),
              true,
            )
            .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
            .jsonPath("$.results[*].overlaps[*].bedspaceId").value(Matchers.not(nonOverlappingBooking.bedspace.id))
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
        val expextedPremisesOneRoomOne = expextedPremisesOne.bedspaces.first()
        val expextedPremisesOneRoomTwo = expextedPremisesOne.bedspaces.drop(1).first()
        val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_MEN_ONLY_ID) }
        val expextedPremisesTwoRoomOne = expextedPremisesTwo.bedspaces.first()
        val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID) }
        val expextedPremisesThreeRoomOne = expextedPremisesThree.bedspaces.first()

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 3,
                resultsBedspaceCount = 4,
                results = listOf(
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomTwo,
                    searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                    bedspaceCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesTwo,
                    expextedPremisesTwoRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      getSharedPropertyCharacteristicPair(),
                      getMenOnlyCharacteristicPair(),
                    ),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesThree,
                    expextedPremisesThreeRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      getSharedPropertyCharacteristicPair(),
                      getWomenOnlyCharacteristicPair(),
                    ),
                    bedspaceCharacteristics = listOf(),
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
        val characteristicOne = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicOne")
        }
        val characteristicTwo = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicTwo")
        }
        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicOne),
        )
        val premisesTwo = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Two",
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicTwo),
        )
        createBedspace(premisesOne, "Bedspace One", listOf())
        createBedspace(premisesTwo, "Bedspace Two", listOf())

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
        val characteristicOne = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicOne")
        }
        val characteristicTwo = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicTwo")
        }
        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicOne),
        )

        val premisesTwo = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Two",
          localAuthorityArea,
          searchPdu,
          mutableListOf(characteristicTwo),
        )

        createBedspace(premisesOne, "Bedspace One", listOf())
        createBedspace(premisesTwo, "Bedspace Two", listOf())

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
        val characteristicOne = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicOne")
        }
        val characteristicTwo = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicTwo")
        }
        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          localAuthorityArea,
          searchPdu,
          mutableListOf(),
        )
        val premisesOneBedOne = createBedspace(premisesOne, "Bedspace One", listOf(characteristicOne))
        val premisesOneBedTwo = createBedspace(premisesOne, "Bedspace Two", listOf(characteristicTwo))

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

        val returnedBedIds = result.results.map { it.bedspace.id }
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
        val characteristicOne = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicOne")
        }
        val characteristicTwo = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicTwo")
        }
        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          localAuthorityArea,
          searchPdu,
          characteristics = mutableListOf(
            cas3PremisesCharacteristicEntityFactory.produceAndPersist {
              name("CharacteristicOne")
            },
          ),
        )

        val premisesOneBedOne = createBedspace(premisesOne, "Bedspace One", listOf(characteristicOne))
        val premisesOneBedTwo = createBedspace(premisesOne, "Bedspace Two", listOf(characteristicTwo))
        val premisesOneBedThree = createBedspace(premisesOne, "Bedspace Three", listOf(characteristicOne, characteristicTwo))

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

        val returnedBedIds = result.results.map { it.bedspace.id }
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
        val premisesCharacteristicOne = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicOne")
        }
        val premisesCharacteristicTwo = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicTwo")
        }
        val premisesCharacteristicThree = cas3PremisesCharacteristicEntityFactory.produceAndPersist {
          name("CharacteristicThree")
        }
        val premisesOne = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises One",
          localAuthorityArea,
          searchPdu,
          mutableListOf(premisesCharacteristicOne),
        )

        val premisesTwo = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Two",
          localAuthorityArea,
          searchPdu,
          mutableListOf(premisesCharacteristicOne, premisesCharacteristicTwo),
        )

        val premisesThree = createPremisesWithCharacteristics(
          UUID.randomUUID(),
          "Premises Three",
          localAuthorityArea,
          searchPdu,
          mutableListOf(premisesCharacteristicOne, premisesCharacteristicTwo, premisesCharacteristicThree),
        )
        val roomCharacteristicOne = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicOne")
        }
        val roomCharacteristicTwo = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicTwo")
        }
        val roomCharacteristicThree = cas3BedspaceCharacteristicEntityFactory.produceAndPersist {
          withName("CharacteristicThree")
        }

        // premises one beds
        val premisesOneBedOne = createBedspace(premisesOne, "11", listOf(roomCharacteristicOne))
        val premisesOneBedTwo = createBedspace(premisesOne, "12", listOf(roomCharacteristicOne, roomCharacteristicTwo))
        createBedspace(premisesOne, "13", listOf(roomCharacteristicOne, roomCharacteristicTwo, roomCharacteristicThree))

        // premises two beds
        val premisesTwoBedOne = createBedspace(premisesTwo, "21", listOf(roomCharacteristicOne))
        val premisesTwoBedTwo = createBedspace(premisesTwo, "22", listOf(roomCharacteristicOne, roomCharacteristicTwo))
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
        val returnedBedIds = result.results.map { it.bedspace.id }
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
        val expextedPremisesOneRoomOne = expextedPremisesOne.bedspaces.first()
        val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID) }
        val expextedPremisesTwoRoomOne = expextedPremisesTwo.bedspaces.first()
        val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
        val expextedPremisesThreeRoomOne = expextedPremisesThree.bedspaces.first()
        val expextedPremisesFour = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID) }
        val expextedPremisesFourRoomOne = expextedPremisesFour.bedspaces.first()

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 4,
                resultsBedspaceCount = 4,
                results = listOf(
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomOne,
                    pduName = searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesTwo,
                    expextedPremisesTwoRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      getSingleOccupancyCharacteristicPair(),
                      getMenOnlyCharacteristicPair(),
                    ),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesThree,
                    expextedPremisesThreeRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                    bedspaceCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesFour,
                    expextedPremisesFourRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      getWomenOnlyCharacteristicPair(),
                      getSingleOccupancyCharacteristicPair(),
                    ),
                    bedspaceCharacteristics = listOf(),
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
        val expextedPremisesOneRoomOne = expextedPremisesOne.bedspaces.drop(1).first()
        val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
        val expextedPremisesTwoRoomOne = expextedPremisesTwo.bedspaces.first()

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 2,
                resultsBedspaceCount = 2,
                results = listOf(
                  createBedspaceSearchResult(
                    expextedPremisesOne,
                    expextedPremisesOneRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                    bedspaceCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    expextedPremisesTwo,
                    expextedPremisesTwoRoomOne,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                    bedspaceCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
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
          val expextedPremisesOneRoomOne = expextedPremisesOne.bedspaces.first()
          val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_ID) }
          val expextedPremisesTwoRoomOne = expextedPremisesTwo.bedspaces.first()
          val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_MEN_ONLY_ID) }
          val expextedPremisesThreeRoomOne = expextedPremisesThree.bedspaces.first()
          val expextedPremisesFour = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID) }
          val expextedPremisesFourRoomOne = expextedPremisesFour.bedspaces.first()
          val expextedPremisesFive = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID) }
          val expextedPremisesFiveRoomOne = expextedPremisesFive.bedspaces.first()

          val offenderDetailsOne = offenders.first().first
          CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsOne)
            .withGender("Male")
            .produce()

          val premisesPubNearBy = premises.first { p -> p.id == UUID.fromString(PREMISES_PUB_NEARBY_ID) }
          val premisesPubNearByBedspaceOne = premisesPubNearBy.bedspaces.first()
          cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesPubNearBy)
            withBedspace(premisesPubNearByBedspaceOne)
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
          val premisesSharedPropertyRoomTwo = premisesSharedProperty.bedspaces.drop(1).first()
          val bookingOffenderTwo = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSharedPropertyRoomTwo.premises)
            withBedspace(premisesSharedPropertyRoomTwo)
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
          val premisesSingleOccupancyWheelchairAccessibleRoomOne = premisesSingleOccupancyWheelchairAccessible.bedspaces.first()
          cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSingleOccupancyWheelchairAccessible)
            withBedspace(premisesSingleOccupancyWheelchairAccessibleRoomOne)
            withArrivalDate(LocalDate.now().minusDays(28))
            withDepartureDate(LocalDate.now().plusDays(34))
            withCrn(offenderDetailsThree.otherIds.crn)
          }

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseSummaryTwo))

          webTestClient.post()
            .uri("cas3/v2/bedspaces/search")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
                Cas3v2BedspaceSearchResults(
                  resultsPremisesCount = 5,
                  resultsBedspaceCount = 5,
                  results = listOf(
                    createBedspaceSearchResult(
                      expextedPremisesOne,
                      expextedPremisesOneRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getMenOnlyCharacteristicPair()),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesTwo,
                      expextedPremisesTwoRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "${offenderDetailsTwo.firstName} ${offenderDetailsTwo.surname}",
                          crn = offenderDetailsTwo.otherIds.crn,
                          personType = PersonType.fullPerson,
                          days = 20,
                          bookingId = bookingOffenderTwo.id,
                          bedspaceId = premisesSharedPropertyRoomTwo.id,
                          isSexualRisk = false,
                          sex = "Male",
                          assessmentId = null,
                        ),
                      ),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesThree,
                      expextedPremisesThreeRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getMenOnlyCharacteristicPair(),
                        getSharedPropertyCharacteristicPair(),
                      ),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFour,
                      expextedPremisesFourRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFive,
                      expextedPremisesFiveRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getSingleOccupancyCharacteristicPair(),
                        getMenOnlyCharacteristicPair(),
                      ),
                      bedspaceCharacteristics = listOf(),
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
          val expextedPremisesOneRoomOne = expextedPremisesOne.bedspaces.first()
          val expextedPremisesTwo = premises.first { p -> p.id == UUID.fromString(PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID) }
          val expextedPremisesTwoRoomOne = expextedPremisesTwo.bedspaces.first()
          val expextedPremisesThree = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID) }
          val expextedPremisesThreeRoomOne = expextedPremisesThree.bedspaces.first()
          val expextedPremisesFour = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID) }
          val expextedPremisesFourRoomOne = expextedPremisesFour.bedspaces.first()
          val expextedPremisesFive = premises.first { p -> p.id == UUID.fromString(PREMISES_WOMEN_ONLY_ID) }
          val expextedPremisesFiveRoomOne = expextedPremisesFive.bedspaces.first()

          val offenderDetailsOne = offenders.first().first
          CaseSummaryFactory()
            .fromOffenderDetails(offenderDetailsOne)
            .withGender("Male")
            .produce()

          val premisesSingleOccupancy = premises.first { p -> p.id == UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID) }
          val premisesSingleOccupancyRoomOne = premisesSingleOccupancy.bedspaces.first()
          cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSingleOccupancy)
            withBedspace(premisesSingleOccupancyRoomOne)
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
          val premisesSharedPropertyRoomTwo = premisesSharedProperty.bedspaces.drop(1).first()
          val bookingOffenderTwo = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesSharedProperty)
            withBedspace(premisesSharedPropertyRoomTwo)
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
          val premisesPubNearbyRoomOne = premisesPubNearby.bedspaces.first()
          cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premisesPubNearby)
            withBedspace(premisesPubNearbyRoomOne)
            withArrivalDate(LocalDate.now().minusDays(3))
            withDepartureDate(LocalDate.now().plusDays(102))
            withCrn(offenderDetailsThree.otherIds.crn)
          }

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseSummaryTwo))

          webTestClient.post()
            .uri("cas3/v2/bedspaces/search")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
                Cas3v2BedspaceSearchResults(
                  resultsPremisesCount = 5,
                  resultsBedspaceCount = 5,
                  results = listOf(
                    createBedspaceSearchResult(
                      expextedPremisesOne,
                      expextedPremisesOneRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(getSharedPropertyCharacteristicPair()),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(
                        Cas3v2BedspaceSearchResultOverlap(
                          name = "${offenderDetailsTwo.firstName} ${offenderDetailsTwo.surname}",
                          crn = offenderDetailsTwo.otherIds.crn,
                          personType = PersonType.fullPerson,
                          days = 66,
                          bookingId = bookingOffenderTwo.id,
                          bedspaceId = premisesSharedPropertyRoomTwo.id,
                          isSexualRisk = false,
                          sex = "Female",
                          assessmentId = null,
                        ),
                      ),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesTwo,
                      expextedPremisesTwoRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getWomenOnlyCharacteristicPair(),
                        getSharedPropertyCharacteristicPair(),
                      ),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesThree,
                      expextedPremisesThreeRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getSingleOccupancyCharacteristicPair()),
                      bedspaceCharacteristics = listOf(getWheelchairAccessibleCharacteristicPair()),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFour,
                      expextedPremisesFourRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(
                        getSingleOccupancyCharacteristicPair(),
                        getWomenOnlyCharacteristicPair(),
                      ),
                      bedspaceCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createBedspaceSearchResult(
                      expextedPremisesFive,
                      expextedPremisesFiveRoomOne,
                      searchPdu.name,
                      numberOfBedspaces = 1,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(getWomenOnlyCharacteristicPair()),
                      bedspaceCharacteristics = listOf(),
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
    fun `Searching for a bedspace should return single bed when given premises has got 2 rooms where one with endDate and another bedspace without end date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.now().plusDays(3)
        val durationDays = 7L
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = cas3PremisesEntityFactory.produceAndPersist {
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withStatus(Cas3PremisesStatus.online)
        }
        cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("not Matching Bed")
          withEndDate(searchStartDate.plusDays(2))
        }

        val bedWithoutEndDate = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("Matching Bed")
        }

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 1,
                resultsBedspaceCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    bedWithoutEndDate,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
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

        val premises = cas3PremisesEntityFactory.produceAndPersist {
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withStatus(Cas3PremisesStatus.online)
        }
        val bed = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premises)
          withReference("Matching Bed")
          withEndDate(searchStartDate.plusDays(durationDays.toLong() + 2))
        }

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 1,
                resultsBedspaceCount = 1,
                results = listOf(
                  createBedspaceSearchResult(
                    premises,
                    bed,
                    searchPdu.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a bedspace should return no bed when given premises has got 2 rooms where one with endDate in the passed and another bedspace with matching end date`() {
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
        val premisesOne = cas3PremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withStatus(Cas3PremisesStatus.online)
        }
        val bedOne = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premisesOne)
          withReference("Bed One")
          withEndDate(searchStartDate.plusDays(20))
        }
        val bedTwo = cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premisesOne)
          withReference("Bed Two")
        }
        val premisesTwo = cas3PremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withStatus(Cas3PremisesStatus.online)
        }
        cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premisesTwo)
          withReference("Bed Three")
          withEndDate(searchStartDate.plusDays(40))
        }
        cas3BedspaceEntityFactory.produceAndPersist {
          withPremises(premisesTwo)
          withReference("Bed Four")
        }

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 1,
                resultsBedspaceCount = 2,
                results = listOf(
                  createBedspaceSearchResult(
                    premisesOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premisesOne,
                    bedTwo,
                    searchPdu.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
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

        val premisesOne = cas3PremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduOne)
          withStatus(Cas3PremisesStatus.online)
        }

        val bedOnePremisesOne = createBedspace(premisesOne, "Bedspace One", listOf())
        val bedTwoPremisesOne = createBedspace(premisesOne, "Bedspace Two", listOf())

        val premisesTwo = cas3PremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withStatus(Cas3PremisesStatus.online)
        }

        createBedspace(premisesTwo, "Bedspace Three", listOf())

        val premisesThree = cas3PremisesEntityFactory.produceAndPersist {
          withName("Premises Three")
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduThree)
          withStatus(Cas3PremisesStatus.online)
        }

        val bedOnePremisesThree = createBedspace(premisesThree, "Bedspace Four", listOf())

        webTestClient.post()
          .uri("cas3/v2/bedspaces/search")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
              Cas3v2BedspaceSearchResults(
                resultsPremisesCount = 2,
                resultsBedspaceCount = 3,
                results = listOf(
                  createBedspaceSearchResult(
                    premisesOne,
                    bedOnePremisesOne,
                    pduOne.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premisesOne,
                    bedTwoPremisesOne,
                    pduOne.name,
                    numberOfBedspaces = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createBedspaceSearchResult(
                    premisesThree,
                    bedOnePremisesThree,
                    pduThree.name,
                    numberOfBedspaces = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    bedspaceCharacteristics = listOf(),
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
        .uri("cas3/v2/bedspaces/search")
        .headers(buildTemporaryAccommodationHeaders(jwt))
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
            Cas3v2BedspaceSearchResults(
              resultsPremisesCount = 0,
              resultsBedspaceCount = 0,
              results = listOf(),
            ),
          ),
        )
    }

    @SuppressWarnings("LongParameterList")
    private fun createPremisesWithCharacteristics(
      premisesId: UUID,
      premisesName: String,
      localAuthorityArea: LocalAuthorityAreaEntity,
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      characteristics: MutableList<Cas3PremisesCharacteristicEntity>,
    ) = cas3PremisesEntityFactory.produceAndPersist {
      withId(premisesId)
      withName(premisesName)
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withStatus(Cas3PremisesStatus.online)
      withCharacteristics(characteristics)
    }

    private fun createBedspace(premises: Cas3PremisesEntity, bedspaceReference: String, bedspaceCharacteristics: List<Cas3BedspaceCharacteristicEntity>): Cas3BedspacesEntity {
      val bedspace = when {
        bedspaceCharacteristics.isEmpty() -> {
          cas3BedspaceEntityFactory.produceAndPersist {
            withReference(bedspaceReference)
            withPremises(premises)
          }
        }

        else -> {
          cas3BedspaceEntityFactory.produceAndPersist {
            withReference(bedspaceReference)
            withPremises(premises)
            withCharacteristics(bedspaceCharacteristics.toMutableList())
          }
        }
      }
      premises.bedspaces.add(bedspace)
      return bedspace
    }

    private fun createPremisesWithBedspaceEndDate(
      searchStartDate: LocalDate,
      bedEndDate: LocalDate,
    ): ProbationDeliveryUnitEntity {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
      val premises = cas3PremisesEntityFactory.produceAndPersist {
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withStatus(Cas3PremisesStatus.online)
      }
      cas3BedspaceEntityFactory.produceAndPersist {
        withPremises(premises)
        withReference("not Matching Bed")
        withEndDate(bedEndDate)
      }
      cas3BedspaceEntityFactory.produceAndPersist {
        withPremises(premises)
        withReference("Not Matching Bed")
        withEndDate(searchStartDate.minusDays(5))
      }
      return searchPdu
    }

    @SuppressWarnings("LongParameterList")
    private fun createBedspaceSearchResult(
      premises: Cas3PremisesEntity,
      bedspace: Cas3BedspacesEntity,
      pduName: String,
      numberOfBedspaces: Int,
      numberOfBookedBeds: Int,
      premisesCharacteristics: List<Cas3CharacteristicPair>,
      bedspaceCharacteristics: List<Cas3CharacteristicPair>,
      overlaps: List<Cas3v2BedspaceSearchResultOverlap>,
    ) = Cas3v2BedspaceSearchResult(
      premises = Cas3BedspaceSearchResultPremisesSummary(
        id = premises.id,
        name = premises.name,
        addressLine1 = premises.addressLine1,
        postcode = premises.postcode,
        probationDeliveryUnitName = pduName,
        characteristics = premisesCharacteristics,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        notes = premises.notes,
        bedspaceCount = numberOfBedspaces,
        bookedBedspaceCount = numberOfBookedBeds,
      ),
      bedspace = Cas3BedspaceSearchResultBedspaceSummary(
        id = bedspace.id,
        reference = bedspace.reference,
        characteristics = bedspaceCharacteristics,
      ),
      overlaps = overlaps,
    )

    private fun createPremisesAndBedspacesWithCharacteristics(
      localAuthorityArea: LocalAuthorityAreaEntity,
      pdu: ProbationDeliveryUnitEntity,
    ): List<Cas3PremisesEntity> {
      // migrates characteristics
      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceModelData)

      val premisesSingleOccupancyCharacteristic = getPremisesSingleOccupancyCharacteristic()
      val premisesSharedPropertyCharacteristic = getPremisesSharedPropertyCharacteristic()
      val premisesMenOnlyCharacteristic = getPremisesMenOnlyCharacteristic()
      val premisesWomenOnlyCharacteristic = getPremisesWomenOnlyCharacteristic()
      val premisesPubNearbyCharacteristic = getPremisesPubNearByCharacteristic()
      val wheelchairAccessibleCharacteristic = getWheelchairAccessibleCharacteristic()

      val premisesSingleOccupancy = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_ID),
        "Premises Single Occupancy",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic!!),
      )
      createBedspace(premisesSingleOccupancy, "Premises Single Occupancy Bedspace", listOf())

      val premisesSingleOccupancyWithWheelchairAccessible = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WHEELCHAIR_ACCESSIBILITY_ID),
        "Premises Single Occupancy with Wheelchair Accessible",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic),
      )
      createBedspace(
        premisesSingleOccupancyWithWheelchairAccessible,
        "Premises Single Occupancy with Wheelchair Accessible Bedspace",
        listOf(wheelchairAccessibleCharacteristic!!),
      )

      val premisesSharedProperty = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SHARED_PROPERTY_ID),
        "Premises Shared Property",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSharedPropertyCharacteristic!!),
      )

      createBedspace(premisesSharedProperty, "Premises Shared Property Bedspace", listOf())
      createBedspace(
        premisesSharedProperty,
        "Premises Shared Property with Wheelchair Accessible Bedspace",
        listOf(wheelchairAccessibleCharacteristic),
      )

      val premisesMenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_MEN_ONLY_ID),
        "Premises Men Only",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic!!),
      )
      createBedspace(premisesMenOnly, "Premises Men Only Bedspace", listOf())

      val premisesWomenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_WOMEN_ONLY_ID),
        "Premises Women Only",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic!!),
      )
      createBedspace(premisesWomenOnly, "Premises Women Only Bedspace", listOf())

      val premisesPubNearby = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_PUB_NEARBY_ID),
        "Premises Pub Nearby",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesPubNearbyCharacteristic!!),
      )
      createBedspace(premisesPubNearby, "Premises Pub Nearby Bedspace", listOf())

      val premisesSingleOccupancyMenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_MEN_ONLY_ID),
        "Premises Single Occupancy - Men Only",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic, premisesSingleOccupancyCharacteristic),
      )
      createBedspace(
        premisesSingleOccupancyMenOnly,
        "Premises Single Occupancy Men Only Bedspace",
        listOf(),
      )

      val premisesSingleOccupancyWomenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SINGLE_OCCUPANCY_WOMEN_ONLY_ID),
        "Premises Single Occupancy - Women Only",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic, premisesSingleOccupancyCharacteristic),
      )
      createBedspace(
        premisesSingleOccupancyWomenOnly,
        "Premises Single Occupancy Women Only Bedspace",
        listOf(),
      )

      val premisesSharedPropertyMenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SHARED_PROPERTY_MEN_ONLY_ID),
        "Premises Shared Property - Men Only",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic, premisesSharedPropertyCharacteristic),
      )
      createBedspace(
        premisesSharedPropertyMenOnly,
        "Premises Shared Property Men Only Bedspace",
        listOf(),
      )

      val premisesSharedPropertyWomenOnly = createPremisesWithCharacteristics(
        UUID.fromString(PREMISES_SHARED_PROPERTY_WOMEN_ONLY_ID),
        "Premises Shared Property - Women Only",
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic, premisesSharedPropertyCharacteristic),
      )
      createBedspace(
        premisesSharedPropertyWomenOnly,
        "Premises Shared Property Women Only Bedspace",
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

    private fun getResponseForRequest(jwt: String, searchParameters: Cas3BedspaceSearchParameters) = webTestClient.post()
      .uri("cas3/v2/bedspaces/search")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .bodyValue(searchParameters)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(Cas3v2BedspaceSearchResults::class.java)
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

    private fun getPremisesSingleOccupancyCharacteristic(): Cas3PremisesCharacteristicEntity? = cas3PremisesCharacteristicRepository.findByName(CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY)
    private fun getPremisesSharedPropertyCharacteristic(): Cas3PremisesCharacteristicEntity? = cas3PremisesCharacteristicRepository.findByName(CAS3_PROPERTY_NAME_SHARED_PROPERTY)
    private fun getPremisesMenOnlyCharacteristic(): Cas3PremisesCharacteristicEntity? = cas3PremisesCharacteristicRepository.findByName(CAS3_PROPERTY_NAME_MEN_ONLY)
    private fun getPremisesWomenOnlyCharacteristic(): Cas3PremisesCharacteristicEntity? = cas3PremisesCharacteristicRepository.findByName(CAS3_PROPERTY_NAME_WOMEN_ONLY)
    private fun getWheelchairAccessibleCharacteristic(): Cas3BedspaceCharacteristicEntity? = cas3BedspaceCharacteristicRepository.findByName(CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE)
    private fun getPremisesPubNearByCharacteristic(): Cas3PremisesCharacteristicEntity? = cas3PremisesCharacteristicRepository.findByName(CAS3_PROPERTY_NAME_PUB_NEAR_BY)

    private fun getSharedPropertyCharacteristicPair() = Cas3CharacteristicPair(name = CAS3_PROPERTY_NAME_SHARED_PROPERTY, description = "Shared property")
    private fun getSingleOccupancyCharacteristicPair() = Cas3CharacteristicPair(name = CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY, description = "Single occupancy")
    private fun getMenOnlyCharacteristicPair() = Cas3CharacteristicPair(name = CAS3_PROPERTY_NAME_MEN_ONLY, description = "Men only")
    private fun getWomenOnlyCharacteristicPair() = Cas3CharacteristicPair(name = CAS3_PROPERTY_NAME_WOMEN_ONLY, description = "Women only")
    private fun getWheelchairAccessibleCharacteristicPair() = Cas3CharacteristicPair(name = CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE, description = "Wheelchair accessible")
  }
}
