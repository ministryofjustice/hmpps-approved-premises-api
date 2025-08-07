package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultBedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime

@SuppressWarnings("LargeClass", "LongParameterList")
class Cas3v2BookingSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching for bookings without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas3/v2/bookings/search")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Searching for CAS3 bookings returns 200 with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create15TestCas3Bookings(userEntity, offenderDetails)
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails)

        callApiAndAssertResponse("/cas3/v2/bookings/search", jwt, expectedResponse, true)
      }
    }
  }

  @Test
  fun `Searching for CAS3 bookings correctly filtered single booking for a specific crn`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val crn = "S121978"
        create15TestCas3Bookings(userEntity, offenderDetails)
        val expectedBookingSearchResult = createTestCas3Bookings(
          userEntity.probationRegion,
          numberOfPremises = 1,
          numberOfBedsInEachPremises = 1,
          crn,
          offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}",
        )
        val expectedResponse = getExpectedResponse(expectedBookingSearchResult, crn, personName = "Unknown")

        apDeliusContextAddListCaseSummaryToBulkResponse(casesSummary = emptyList(), crns = listOf("S121978"))
        apDeliusContextAddResponseToUserAccessCall(casesAccess = emptyList(), username = userEntity.deliusUsername)

        // when CRN is upper case
        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=$crn", jwt, expectedResponse, true)

        // when CRN is lower case
        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=${crn.lowercase()}", jwt, expectedResponse, true)
      }
    }
  }

  @Test
  fun `Searching for CAS3 single booking for a specific crn where the offender is LAO`() {
    givenAUser(qualifications = listOf(UserQualification.LAO)) { userEntity, jwt ->
      val crn = "S121978"
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn(crn)
          withCurrentRestriction(true)
        },
      ) { offenderDetails, _ ->
        val expectedBookingSearchResult =
          createTestCas3Bookings(userEntity.probationRegion, 1, 1, crn, "Limited Access Offender")
        val expectedResponse = getExpectedResponse(expectedBookingSearchResult, crn, "Limited Access Offender")

        val userExcludedCaseSummary = CaseSummaryFactory()
          .fromOffenderDetails(offenderDetails)
          .withPnc(offenderDetails.otherIds.pncNumber)
          .withCrn(offenderDetails.otherIds.crn)
          .withCurrentExclusion(true)
          .produce()

        apDeliusContextAddListCaseSummaryToBulkResponse(listOf(userExcludedCaseSummary))

        apDeliusContextAddResponseToUserAccessCall(
          listOf(
            CaseAccessFactory()
              .withCrn(userExcludedCaseSummary.crn)
              .withUserExcluded(true)
              .produce(),
          ),
          username = userEntity.deliusUsername,
        )

        // when CRN is upper case
        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=$crn", jwt, expectedResponse, true)

        // when CRN is lower case
        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=${crn.lowercase()}", jwt, expectedResponse, true)
      }
    }
  }

  @Test
  fun `Searching for CAS3 bookings correctly filtered multiple booking for a specific crn`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val crn = "S121978"
        val expectedBookingInSearchResult =
          create15TestCas3Bookings(userEntity, offenderDetails)
        createTestCas3Bookings(userEntity.probationRegion, numberOfPremises = 1, numberOfBedsInEachPremises = 1, crn, offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}")
        val expectedResponse = getExpectedResponse(expectedBookingInSearchResult, offenderDetails)

        // when CRN is upper case
        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=${offenderDetails.otherIds.crn}", jwt, expectedResponse, true)

        // when CRN is lower case
        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=${offenderDetails.otherIds.crn.lowercase()}", jwt, expectedResponse, true)
      }
    }
  }

  @ParameterizedTest
  @CsvSource("S121978", "PersonName")
  fun `Searching for CAS3 bookings with crn or name not exists in the database return empty response`(queryParameterValue: String) {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        create15TestCas3Bookings(userEntity, offenderDetails)
        val expectedBookingSearchResults = Cas3BookingSearchResults(resultsCount = 0, results = emptyList())

        callApiAndAssertResponse("/cas3/v2/bookings/search?crnOrName=$queryParameterValue", jwt, expectedBookingSearchResults, true)
      }
    }
  }

  @Test
  fun `Searching for CAS3 bookings correctly filtered multiple booking when name is used in crnOrName query parameter`() {
    val firstname = "Someuniquename"
    val surname = "Someotheruniqueuniquesurname"
    givenAUser { userEntity, jwt ->
      givenSomeOffenders { offenderSequence ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName(firstname)
            withLastName(surname)
          },
        ) { offenderWithFixedName, _ ->

          val offendersDetailSummary =
            offenderSequence.take(10).map { (offenderDetailSummary, _) -> offenderDetailSummary }.toList()
          val allBookings = mutableListOf<Cas3BookingEntity>()
          val temporaryAccommodationApplications = mutableListOf<TemporaryAccommodationApplicationEntity>()

          offendersDetailSummary.forEach {
            setupApplicationData(it, userEntity, temporaryAccommodationApplications, allBookings)
          }

          setupApplicationData(
            offenderWithFixedName,
            userEntity,
            temporaryAccommodationApplications,
            allBookings,
          )

          // full name match
          var expectedOffender = offendersDetailSummary.drop(2).first()
          var expectedBookings = allBookings.filter { b -> b.crn == expectedOffender.otherIds.crn }
          var expectedResponse = getExpectedResponse(expectedBookings, expectedOffender)

          callApiAndAssertResponse(
            "/cas3/v2/bookings/search?crnOrName=${expectedOffender.firstName} ${expectedOffender.surname}",
            jwt,
            expectedResponse,
            true,
          )

          // first name match
          expectedOffender = offendersDetailSummary.drop(4).first()
          expectedBookings = allBookings.filter { b -> b.crn == expectedOffender.otherIds.crn }
          expectedResponse = getExpectedResponse(expectedBookings, expectedOffender)

          callApiAndAssertResponse(
            "/cas3/v2/bookings/search?crnOrName=${expectedOffender.firstName}",
            jwt,
            expectedResponse,
            true,
          )

          // surname match
          expectedOffender = offendersDetailSummary.drop(7).first()
          expectedBookings = allBookings.filter { b -> b.crn == expectedOffender.otherIds.crn }
          expectedResponse = getExpectedResponse(expectedBookings, expectedOffender)

          callApiAndAssertResponse(
            "/cas3/v2/bookings/search?crnOrName=${expectedOffender.surname}",
            jwt,
            expectedResponse,
            true,
          )

          // partial match
          expectedOffender = offenderWithFixedName
          expectedBookings = allBookings.filter { b -> b.crn == expectedOffender.otherIds.crn }
          expectedResponse = getExpectedResponse(expectedBookings, expectedOffender)

          callApiAndAssertResponse(
            "/cas3/v2/bookings/search?crnOrName=uniquename Someother",
            jwt,
            expectedResponse,
            true,
          )
        }
      }
    }
  }

  private fun setupApplicationData(
    it: OffenderDetailSummary,
    userEntity: UserEntity,
    temporaryAccommodationApplications: MutableList<TemporaryAccommodationApplicationEntity>,
    allBookings: MutableList<Cas3BookingEntity>,
  ) {
    val offenderName = "${it.firstName} ${it.surname}"
    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withName(offenderName)
      withCrn(it.otherIds.crn)
      withProbationRegion(userEntity.probationRegion)
      withCreatedByUser(userEntity)
    }
    temporaryAccommodationApplications += application

    val booking = createTestCas3Bookings(userEntity.probationRegion, 1, 1, it.otherIds.crn, offenderName)
    allBookings += booking
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "PERSON_NAME,asc,departed",
      "PERSON_NAME,desc,departed",
    ],
  )
  fun `Results for departed offenders are sorted correctly for both ascending and descending order`(
    bookingSearchSort: Cas3BookingSearchSortField,
    sortDirection: SortDirection,
    bookingStatus: Cas3BookingStatus,
  ) {
    givenAUser { userEntity, jwt ->
      givenSomeOffenders { offenderSequence ->
        val totalResults = 15
        val pageSize = 10

        val allPremises = cas3PremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationDeliveryUnit(
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(userEntity.probationRegion)
            },
          )
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        }

        allPremises.forEach { premises ->
          cas3BedspaceEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }
        }

        val offenders = offenderSequence.take(totalResults).toList()
        val temporaryAccommodationApplications = mutableListOf<TemporaryAccommodationApplicationEntity>()
        val allBookings: List<Cas3BookingEntity> = offenders.map {
          val offenderName = "${it.first.firstName} ${it.first.surname}"
          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withName(offenderName)
            withCrn(it.first.otherIds.crn)
            withProbationRegion(userEntity.probationRegion)
            withCreatedByUser(userEntity)
          }

          temporaryAccommodationApplications += application

          val booking = createBooking(
            bedspace = createTestCas3Bedspace(userEntity.probationRegion),
            crn = it.first.otherIds.crn,
            arrivalDate = LocalDate.now().minusDays(randomInt(5, 20).toLong()),
            departureDate = LocalDate.now().plusDays(3),
            createdAt = LocalDate.now().minusDays(randomInt(10, 20).toLong()).toLocalDateTime(),
            bookingStatus,
            offenderName,
          )
          val departure = cas3DepartureEntityFactory.produceAndPersist {
            withBooking(booking)
            withYieldedReason {
              departureReasonEntityFactory.produceAndPersist()
            }
            withYieldedMoveOnCategory {
              moveOnCategoryEntityFactory.produceAndPersist()
            }
          }
          booking.departures.add(departure)

          booking
        }

        var offendersSorted: List<Pair<OffenderDetailSummary, InmateDetail>> = mutableListOf()
        var responseSorted: Cas3BookingSearchResults? = null

        when (sortDirection) {
          SortDirection.asc -> {
            offendersSorted = offenders.sortedBy { it.first.firstName }
            responseSorted = Cas3BookingSearchResults(
              offenders.size,
              getExpectedResponseList(allBookings, offenders.map { it.first }).results.sortedBy { it.person.name },
            )
          }

          SortDirection.desc -> {
            offendersSorted = offenders.sortedByDescending { it.first.firstName }
            responseSorted = Cas3BookingSearchResults(
              offenders.size,
              getExpectedResponseList(allBookings, offenders.map { it.first }).results.sortedByDescending { it.person.name },
            )
          }
        }

        val totalPages = 2
        for (page in 0..<totalPages) {
          val currentPageSize = if (page == totalPages - 1) totalResults % pageSize else pageSize

          apDeliusContextAddListCaseSummaryToBulkResponse(
            casesSummary = offendersSorted.drop(page * pageSize)
              .take(pageSize)
              .map { it.first.asCaseSummary() },
          )

          val expectedPageResponse = Cas3BookingSearchResults(currentPageSize, responseSorted.results.subList(page * pageSize, page * pageSize + currentPageSize))
          val apiUri = "/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=${bookingSearchSort.value}&status=departed&page=${page + 1}"
          webTestClient.get()
            .uri(apiUri)
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", page + 1.toLong())
            .expectHeader().valueEquals("X-Pagination-TotalPages", totalPages.toLong())
            .expectHeader().valueEquals("X-Pagination-TotalResults", totalResults.toLong())
            .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
            .expectBody()
            .json(objectMapper.writeValueAsString(expectedPageResponse), true)
        }
      }
    }
  }

  @Test
  fun `Results are filtered by booking status when query parameter is supplied`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->

        val allPremises = cas3PremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationDeliveryUnit(
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(userEntity.probationRegion)
            },
          )
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        }

        val allBedspaces = allPremises.map { premises ->
          cas3BedspaceEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }
        }.flatten()

        val allBookings = allBedspaces.mapIndexed { index, bedspace ->
          when (index % 5) {
            // Provisional
            0 -> {
              cas3BookingEntityFactory.produceAndPersist {
                withPremises(bedspace.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBedspace(bedspace)
                withStatus(Cas3BookingStatus.provisional)
                withServiceName(ServiceName.temporaryAccommodation)
              }
            }
            // Confirmed
            1 -> {
              val booking = cas3BookingEntityFactory.produceAndPersist {
                withPremises(bedspace.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBedspace(bedspace)
                withStatus(Cas3BookingStatus.confirmed)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
                withBooking(booking)
              }
              booking.confirmation = confirmation
              booking
            }
            // Active
            2 -> {
              val booking = cas3BookingEntityFactory.produceAndPersist {
                withPremises(bedspace.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBedspace(bedspace)
                withStatus(Cas3BookingStatus.arrived)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val arrival = cas3ArrivalEntityFactory.produceAndPersist {
                withBooking(booking)
              }
              booking.arrivals.add(arrival)
              booking
            }
            // Closed
            3 -> {
              val booking = cas3BookingEntityFactory.produceAndPersist {
                withPremises(bedspace.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBedspace(bedspace)
                withStatus(Cas3BookingStatus.closed)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val departure = cas3DepartureEntityFactory.produceAndPersist {
                withBooking(booking)
                withYieldedReason {
                  departureReasonEntityFactory.produceAndPersist()
                }
                withYieldedMoveOnCategory {
                  moveOnCategoryEntityFactory.produceAndPersist()
                }
              }
              booking.departures.add(departure)
              booking
            }
            // Cancelled
            4 -> {
              val booking = cas3BookingEntityFactory.produceAndPersist {
                withPremises(bedspace.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBedspace(bedspace)
                withStatus(Cas3BookingStatus.cancelled)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val cancellation = cas3CancellationEntityFactory.produceAndPersist {
                withBooking(booking)
                withYieldedReason {
                  cancellationReasonEntityFactory.produceAndPersist()
                }
              }
              booking.cancellations.add(cancellation)
              booking
            }
            else -> {
              cas3BookingEntityFactory.produceAndPersist {
                withPremises(bedspace.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBedspace(bedspace)
                withStatus(Cas3BookingStatus.provisional)
                withServiceName(ServiceName.temporaryAccommodation)
              }
            }
          }
        }

        val expectedBookings = allBookings.filter { it.cancellation != null }

        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails)

        callApiAndAssertResponse("/cas3/v2/bookings/search?status=cancelled", jwt, expectedResponse, false)
      }
    }
  }

  @Test
  fun `Results are only returned for the user's probation region for Temporary Accommodation`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val expectedPremises = cas3PremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationDeliveryUnit(
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(userEntity.probationRegion)
            },
          )
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        }

        val unexpectedPremises = cas3PremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationDeliveryUnit(
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(probationRegionEntityFactory.produceAndPersist())
            },
          )
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        }

        val allPremises = expectedPremises + unexpectedPremises

        val allBedspaces = allPremises.map { premises ->
          cas3BedspaceEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }
        }.flatten()

        val allBookings = allBedspaces.map {
          cas3BookingEntityFactory.produceAndPersist {
            withPremises(it.premises)
            withCrn(offenderDetails.otherIds.crn)
            withBedspace(it)
            withServiceName(ServiceName.temporaryAccommodation)
          }
        }

        val expectedPremisesIds = expectedPremises.map { it.id }
        val expectedBookings = allBookings.filter { expectedPremisesIds.contains(it.premises.id) }
        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails)
        callApiAndAssertResponse("/cas3/v2/bookings/search", jwt, expectedResponse, false)
      }
    }
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "PERSON_CRN,asc",
      "BOOKING_START_DATE,asc",
      "BOOKING_END_DATE,asc",
      "BOOKING_CREATED_AT,asc",
      "PERSON_CRN,desc",
      "BOOKING_START_DATE,desc",
      "BOOKING_END_DATE,desc",
      "BOOKING_CREATED_AT,desc",
    ],
  )
  fun `Searching for Temporary Accommodation bookings with pagination returns 200 with correct subset of results`(
    bookingSearchSort: Cas3BookingSearchSortField,
    sortDirection: SortDirection,
  ) {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create15TestCas3Bookings(userEntity, offenderDetails).toMutableList()
        getSortedBooking(allBookings, bookingSearchSort, sortDirection)
        val firstPage = allBookings.subList(0, 10)
        val secondPage = allBookings.subList(10, allBookings.size)
        val expectedFirstPageResponse = getExpectedResponse(firstPage, offenderDetails)
        val expectedSecondPageResponse = getExpectedResponse(secondPage, offenderDetails)

        webTestClient.get()
          .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=${bookingSearchSort.value}&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedFirstPageResponse))

        webTestClient.get()
          .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=${bookingSearchSort.value}&page=2")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedSecondPageResponse))
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = SortDirection::class)
  fun `Results are ordered by the start date when multiple booking have the same arrival date when the query parameters are supplied with Pagination`(
    sortDirection: SortDirection,
  ) {
    givenAUser { userEntity, jwt ->
      val crns = mutableListOf<String>()
      repeat(15) { crns += randomStringMultiCaseWithNumbers(8) }

      val temporaryAccommodationApplications = mutableListOf<TemporaryAccommodationApplicationEntity>()
      val offendersCrnAndName = crns.associateBy(
        keySelector = { it },
        valueTransform = { NameFactory().produce() },
      )

      val allBookings = crns.map {
        val offenderName = "${offendersCrnAndName[it]?.forename} ${offendersCrnAndName[it]?.surname}"
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withName(offenderName)
          withCrn(it)
          withProbationRegion(userEntity.probationRegion)
          withCreatedByUser(userEntity)
        }
        temporaryAccommodationApplications += application

        val bed = createTestCas3Bedspace(userEntity.probationRegion)
        createBooking(
          bed,
          it,
          LocalDate.now().minusDays(5),
          LocalDate.now().plusDays(randomInt(1, 10).toLong()),
          LocalDate.now().minusDays(randomInt(10, 20).toLong()).toLocalDateTime(),
          offenderName = offenderName,
        )
      }

      // Assert bookings page 1
      mockApDeliusContextCasesSummary(temporaryAccommodationApplications, offendersCrnAndName, userEntity.deliusUsername, sortDirection, 10, 0)

      var bookingSearchResults = getExpectedResponse(allBookings, temporaryAccommodationApplications)
      var expectedResponse = Cas3BookingSearchResults(
        10,
        if (sortDirection == SortDirection.asc) {
          bookingSearchResults.results.sortedBy { it.booking.startDate }.sortedBy { it.person.name }.take(10)
        } else {
          bookingSearchResults.results.sortedByDescending { it.booking.startDate }.sortedByDescending { it.person.name }.take(10)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=startDate&page=1&status=provisional")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
        .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
        .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
        .expectHeader().valueEquals("X-Pagination-PageSize", 10)
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse), true)

      // Assert bookings page 2
      mockApDeliusContextCasesSummary(temporaryAccommodationApplications, offendersCrnAndName, userEntity.deliusUsername, sortDirection, 5, 10)

      bookingSearchResults = getExpectedResponse(allBookings, temporaryAccommodationApplications)
      expectedResponse = Cas3BookingSearchResults(
        5,
        if (sortDirection == SortDirection.asc) {
          bookingSearchResults.results.sortedBy { it.booking.startDate }.sortedBy { it.person.name }.drop(10)
        } else {
          bookingSearchResults.results.sortedByDescending { it.booking.startDate }.sortedByDescending { it.person.name }.drop(10)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=startDate&page=2&status=provisional")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
        .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
        .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
        .expectHeader().valueEquals("X-Pagination-PageSize", 10)
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse), true)
    }
  }

  @ParameterizedTest
  @EnumSource(value = SortDirection::class)
  fun `Results are ordered by the end date when multiple booking have the same arrival date when the query parameters are supplied with Pagination`(
    sortDirection: SortDirection,
  ) {
    givenAUser { userEntity, jwt ->
      val crns = mutableListOf<String>()
      repeat(15) { crns += randomStringMultiCaseWithNumbers(8) }

      val temporaryAccommodationApplications = mutableListOf<TemporaryAccommodationApplicationEntity>()
      val offendersCrnAndName = crns.associateBy(
        keySelector = { it },
        valueTransform = { NameFactory().produce() },
      )

      val allBookings = crns.map {
        val offenderName = "${offendersCrnAndName[it]?.forename} ${offendersCrnAndName[it]?.surname}"
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withName(offenderName)
          withCrn(it)
          withProbationRegion(userEntity.probationRegion)
          withCreatedByUser(userEntity)
        }
        temporaryAccommodationApplications += application

        val booking = createBooking(
          bedspace = createTestCas3Bedspace(userEntity.probationRegion),
          crn = it,
          arrivalDate = LocalDate.now().minusDays(randomInt(5, 20).toLong()),
          departureDate = LocalDate.now().plusDays(3),
          createdAt = LocalDate.now().minusDays(randomInt(10, 20).toLong()).toLocalDateTime(),
          offenderName = offenderName,
        )
        booking
      }

      // Assert bookings page 1
      mockApDeliusContextCasesSummary(temporaryAccommodationApplications, offendersCrnAndName, userEntity.deliusUsername, sortDirection, 10, 0)

      var bookingSearchResults = getExpectedResponse(allBookings, temporaryAccommodationApplications)
      var expectedResponse = Cas3BookingSearchResults(
        10,
        if (sortDirection == SortDirection.asc) {
          bookingSearchResults.results.sortedBy { it.booking.endDate }.sortedBy { it.person.name }.take(10)
        } else {
          bookingSearchResults.results.sortedByDescending { it.booking.endDate }.sortedByDescending { it.person.name }.take(10)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=endDate&page=1&status=provisional")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
        .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
        .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
        .expectHeader().valueEquals("X-Pagination-PageSize", 10)
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse), true)

      // Assert bookings page 2
      mockApDeliusContextCasesSummary(temporaryAccommodationApplications, offendersCrnAndName, userEntity.deliusUsername, sortDirection, 5, 10)

      bookingSearchResults = getExpectedResponse(allBookings, temporaryAccommodationApplications)
      expectedResponse = Cas3BookingSearchResults(
        5,
        if (sortDirection == SortDirection.asc) {
          bookingSearchResults.results.sortedBy { it.booking.endDate }.sortedBy { it.person.name }.drop(10)
        } else {
          bookingSearchResults.results.sortedByDescending { it.booking.endDate }.sortedByDescending { it.person.name }.drop(10)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=endDate&page=2&status=provisional")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
        .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
        .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
        .expectHeader().valueEquals("X-Pagination-PageSize", 10)
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse), true)
    }
  }

  @Test
  fun `Results are ordered by the person crn and sorted descending order when the query parameters are supplied with Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestCas3Bookings(userEntity, offenderDetails)
        val sortedByDescending = allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails)

        webTestClient.get()
          .uri("/cas3/v2/bookings/search?sortDirection=descending&sortField=crn&page=1&status=provisional")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 10)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Get all results ordered by the person crn in descending order when the query parameters supplied without Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create15TestCas3Bookings(userEntity, offenderDetails)
        val sortedByDescending = allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails)

        callApiAndAssertResponse("/cas3/v2/bookings/search?sortDirection=descending&sortField=crn&status=provisional", jwt, expectedResponse, true)
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = SortDirection::class)
  fun `Results are ordered by the person name when the query parameters are supplied with Pagination`(
    sortDirection: SortDirection,
  ) {
    givenAUser { userEntity, jwt ->
      val crns = mutableListOf<String>()
      repeat(15) { crns += randomStringMultiCaseWithNumbers(8) }

      val temporaryAccommodationApplications = mutableListOf<TemporaryAccommodationApplicationEntity>()
      val offendersCrnAndName = crns.associateBy(
        keySelector = { it },
        valueTransform = { NameFactory().produce() },
      )

      val allBookings = crns.map {
        val offenderName = "${offendersCrnAndName[it]?.forename} ${offendersCrnAndName[it]?.surname}"
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withName(offenderName)
          withCrn(it)
          withProbationRegion(userEntity.probationRegion)
          withCreatedByUser(userEntity)
        }
        temporaryAccommodationApplications += application
        createTestCas3Bookings(userEntity.probationRegion, 1, 1, it, offenderName)
      }.flatten()

      // Assert bookings page 1
      mockApDeliusContextCasesSummary(temporaryAccommodationApplications, offendersCrnAndName, userEntity.deliusUsername, sortDirection, 10, 0)

      var bookingSearchResults = getExpectedResponse(allBookings, temporaryAccommodationApplications)
      var expectedResponse = Cas3BookingSearchResults(
        10,
        if (sortDirection == SortDirection.asc) {
          bookingSearchResults.results.sortedBy { it.person.name }.take(10)
        } else {
          bookingSearchResults.results.sortedByDescending { it.person.name }.take(10)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=name&page=1&status=provisional")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
        .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
        .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
        .expectHeader().valueEquals("X-Pagination-PageSize", 10)
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse), true)

      // Assert bookings page 2
      mockApDeliusContextCasesSummary(temporaryAccommodationApplications, offendersCrnAndName, userEntity.deliusUsername, sortDirection, 5, 10)

      bookingSearchResults = getExpectedResponse(allBookings, temporaryAccommodationApplications)
      expectedResponse = Cas3BookingSearchResults(
        5,
        if (sortDirection == SortDirection.asc) {
          bookingSearchResults.results.sortedBy { it.person.name }.drop(10)
        } else {
          bookingSearchResults.results.sortedByDescending { it.person.name }.drop(10)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/bookings/search?sortDirection=$sortDirection&sortField=name&page=2&status=provisional")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
        .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
        .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
        .expectHeader().valueEquals("X-Pagination-PageSize", 10)
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse), true)
    }
  }

  @Test
  fun `No Results returned when searching for cancelled booking status and all existing bookings are confirmed`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        create10TestCas3Bookings(userEntity, offenderDetails)
        val expectedResponse = getExpectedResponse(emptyList(), offenderDetails)

        webTestClient.get()
          .uri("/cas3/v2/bookings/search?sortDirection=descending&sortField=crn&page=1&status=cancelled")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 0)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 0)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  private fun getExpectedResponseList(
    expectedBookings: List<Cas3BookingEntity>,
    offenderDetails: List<OffenderDetailSummary>,
  ) = Cas3BookingSearchResults(
    resultsCount = expectedBookings.size,
    results = expectedBookings.map { booking -> bookingSearchMapping(offenderDetails.first { it.otherIds.crn == booking.crn }, booking) },
  )

  private fun getExpectedResponse(
    expectedBookings: List<Cas3BookingEntity>,
    offenderDetails: OffenderDetailSummary,
  ) = Cas3BookingSearchResults(
    resultsCount = expectedBookings.size,
    results = expectedBookings.map { booking ->
      bookingSearchMapping(offenderDetails, booking)
    },
  )

  private fun bookingSearchMapping(
    offenderDetails: OffenderDetailSummary,
    booking: Cas3BookingEntity,
  ) = Cas3BookingSearchResult(
    person = Cas3BookingSearchResultPersonSummary(
      name = "${offenderDetails.firstName} ${offenderDetails.surname}",
      crn = offenderDetails.otherIds.crn,
    ),
    booking = Cas3BookingSearchResultBookingSummary(
      id = booking.id,
      status = when {
        booking.cancellation != null -> Cas3BookingStatus.cancelled
        booking.departure != null -> Cas3BookingStatus.departed
        booking.arrival != null -> Cas3BookingStatus.arrived
        booking.nonArrival != null -> Cas3BookingStatus.notMinusArrived
        booking.confirmation != null -> Cas3BookingStatus.confirmed
        else -> Cas3BookingStatus.provisional
      },
      startDate = booking.arrivalDate,
      endDate = booking.departureDate,
      createdAt = booking.createdAt.toInstant(),
    ),
    premises = Cas3BookingSearchResultPremisesSummary(
      id = booking.premises.id,
      name = booking.premises.name,
      addressLine1 = booking.premises.addressLine1,
      addressLine2 = booking.premises.addressLine2,
      town = booking.premises.town,
      postcode = booking.premises.postcode,
    ),
    bedspace = Cas3BookingSearchResultBedspaceSummary(
      id = booking.bedspace.id,
      reference = booking.bedspace.reference,
    ),
  )

  private fun getExpectedResponse(
    expectedBookings: List<Cas3BookingEntity>,
    temporaryAccommodationApplications: List<TemporaryAccommodationApplicationEntity>,
  ) = Cas3BookingSearchResults(
    resultsCount = expectedBookings.size,
    results = expectedBookings.map { booking ->
      val userApplication = temporaryAccommodationApplications.firstOrNull { a -> a.crn == booking.crn }
      Cas3BookingSearchResult(
        person = Cas3BookingSearchResultPersonSummary(
          name = userApplication?.name,
          crn = booking.crn,
        ),
        booking = Cas3BookingSearchResultBookingSummary(
          id = booking.id,
          status = when {
            booking.cancellation != null -> Cas3BookingStatus.cancelled
            booking.departure != null -> Cas3BookingStatus.departed
            booking.arrival != null -> Cas3BookingStatus.arrived
            booking.nonArrival != null -> Cas3BookingStatus.notMinusArrived
            booking.confirmation != null -> Cas3BookingStatus.confirmed
            else -> Cas3BookingStatus.provisional
          },
          startDate = booking.arrivalDate,
          endDate = booking.departureDate,
          createdAt = booking.createdAt.toInstant(),
        ),
        premises = Cas3BookingSearchResultPremisesSummary(
          id = booking.premises.id,
          name = booking.premises.name,
          addressLine1 = booking.premises.addressLine1,
          addressLine2 = booking.premises.addressLine2,
          town = booking.premises.town,
          postcode = booking.premises.postcode,
        ),
        bedspace = Cas3BookingSearchResultBedspaceSummary(
          id = booking.bedspace.id,
          reference = booking.bedspace.reference,
        ),
      )
    },
  )

  private fun getExpectedResponse(
    expectedBookings: List<Cas3BookingEntity>,
    crn: String,
    personName: String,
  ) = Cas3BookingSearchResults(
    resultsCount = expectedBookings.size,
    results = expectedBookings.map { booking ->
      Cas3BookingSearchResult(
        person = Cas3BookingSearchResultPersonSummary(
          name = personName,
          crn = crn,
        ),
        booking = Cas3BookingSearchResultBookingSummary(
          id = booking.id,
          status = when {
            booking.cancellation != null -> Cas3BookingStatus.cancelled
            booking.departure != null -> Cas3BookingStatus.departed
            booking.arrival != null -> Cas3BookingStatus.arrived
            booking.nonArrival != null -> Cas3BookingStatus.notMinusArrived
            booking.confirmation != null -> Cas3BookingStatus.confirmed
            else -> Cas3BookingStatus.provisional
          },
          startDate = booking.arrivalDate,
          endDate = booking.departureDate,
          createdAt = booking.createdAt.toInstant(),
        ),
        premises = Cas3BookingSearchResultPremisesSummary(
          id = booking.premises.id,
          name = booking.premises.name,
          addressLine1 = booking.premises.addressLine1,
          addressLine2 = booking.premises.addressLine2,
          town = booking.premises.town,
          postcode = booking.premises.postcode,
        ),
        bedspace = Cas3BookingSearchResultBedspaceSummary(
          id = booking.bedspace.id,
          reference = booking.bedspace.reference,
        ),
      )
    },
  )

  private fun create10TestCas3Bookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): List<Cas3BookingEntity> = createTestCas3Bookings(
    probationRegion = userEntity.probationRegion,
    numberOfPremises = 5,
    numberOfBedsInEachPremises = 2,
    crn = offenderDetails.otherIds.crn,
    offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}",
  )

  private fun create15TestCas3Bookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): List<Cas3BookingEntity> = createTestCas3Bookings(
    probationRegion = userEntity.probationRegion,
    numberOfPremises = 3,
    numberOfBedsInEachPremises = 5,
    crn = offenderDetails.otherIds.crn,
    offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}",
  )

  private fun createTestCas3Bookings(
    probationRegion: ProbationRegionEntity,
    numberOfPremises: Int,
    numberOfBedsInEachPremises: Int,
    crn: String,
    offenderName: String,
  ): List<Cas3BookingEntity> {
    val allPremises = cas3PremisesEntityFactory.produceAndPersistMultiple(numberOfPremises) {
      withProbationDeliveryUnit(
        probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    val allBedspaces = allPremises.map { premises ->
      cas3BedspaceEntityFactory.produceAndPersistMultiple(numberOfBedsInEachPremises) {
        withPremises(premises)
      }
    }.flatten()

    return allBedspaces.mapIndexed { index, bed ->
      createBooking(
        bed,
        crn,
        LocalDate.now().minusDays((60 - index).toLong()),
        LocalDate.now().minusDays((30 - index).toLong()),
        LocalDate.now().minusDays((30 - index).toLong()).toLocalDateTime(),
        offenderName = offenderName,
      )
    }
  }

  private fun createTestCas3Bedspace(probationRegion: ProbationRegionEntity): Cas3BedspacesEntity {
    val premises = cas3PremisesEntityFactory.produceAndPersist {
      withProbationDeliveryUnit(
        probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    return cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
    }
  }

  @Suppress("LongParameterList")
  private fun createBooking(
    bedspace: Cas3BedspacesEntity,
    crn: String,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    createdAt: OffsetDateTime,
    bookingStatus: Cas3BookingStatus = Cas3BookingStatus.provisional,
    offenderName: String,
  ): Cas3BookingEntity = cas3BookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withPremises(bedspace.premises)
    withBedspace(bedspace)
    withStatus(bookingStatus)
    withServiceName(ServiceName.temporaryAccommodation)
    withArrivalDate(arrivalDate)
    withDepartureDate(departureDate)
    withCreatedAt(createdAt)
    withOffenderName(offenderName)
  }

  private fun callApiAndAssertResponse(
    uri: String,
    jwt: String,
    expectedResponse: Cas3BookingSearchResults,
    jsonStrictMatch: Boolean,
  ) {
    webTestClient.get()
      .uri(uri)
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(objectMapper.writeValueAsString(expectedResponse), jsonStrictMatch)
  }

  private fun mockApDeliusContextCasesSummary(
    applications: MutableList<TemporaryAccommodationApplicationEntity>,
    offendersCrnAndName: Map<String, Name>,
    username: String,
    sortDirection: SortDirection,
    take: Int,
    skip: Int,
  ) {
    val cases = if (sortDirection == SortDirection.asc) {
      applications.sortedBy { it.name }.drop(skip).take(take).map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(offendersCrnAndName[it.crn]!!)
          .produce()
      }
    } else {
      applications.sortedByDescending { it.name }.drop(skip).take(take).map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(offendersCrnAndName[it.crn]!!)
          .produce()
      }
    }

    apDeliusContextAddListCaseSummaryToBulkResponse(cases)
    apDeliusContextAddResponseToUserAccessCall(
      casesAccess = cases.map { CaseAccessFactory().withCrn(it.crn).withAccess().produce() },
      username = username,
    )
  }

  private fun getSortedBooking(bookings: MutableList<Cas3BookingEntity>, sortBy: Cas3BookingSearchSortField, sortDirection: SortDirection) = when (sortBy) {
    Cas3BookingSearchSortField.BOOKING_END_DATE -> sortBookings(Cas3BookingEntity::departureDate, bookings, sortDirection)
    Cas3BookingSearchSortField.BOOKING_START_DATE -> sortBookings(Cas3BookingEntity::arrivalDate, bookings, sortDirection)
    Cas3BookingSearchSortField.PERSON_CRN -> sortBookings(Cas3BookingEntity::crn, bookings, sortDirection)
    else -> sortBookings(Cas3BookingEntity::createdAt, bookings, sortDirection)
  }

  fun <T : Comparable<T>> sortBookings(fn: Cas3BookingEntity.() -> T, bookings: MutableList<Cas3BookingEntity>, sortDirection: SortDirection) = when (sortDirection) {
    SortDirection.asc -> bookings.sortBy { it.fn() }
    else -> bookings.sortByDescending { it.fn() }
  }
}
