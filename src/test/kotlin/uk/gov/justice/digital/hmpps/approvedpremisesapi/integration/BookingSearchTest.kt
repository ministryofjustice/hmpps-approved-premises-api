package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate

class BookingSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching for bookings without JWT returns 401`() {
    webTestClient.get()
      .uri("/bookings/search")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Searching for Approved Premises bookings returns 200 with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = createApprovedPremisesBookingEntities(userEntity, offenderDetails)
        create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Approved Premises bookings with pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = createApprovedPremisesBookingEntities(userEntity, offenderDetails)
        create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        allBookings.sortBy { it.createdAt }
        val firstPage = allBookings.subList(0, 10)
        val secondPage = allBookings.subList(10, allBookings.size)
        val expectedFirstPageResponse = getExpectedResponse(firstPage, offenderDetails)
        val expectedSecondPageResponse = getExpectedResponse(secondPage, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
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
          .uri("/bookings/search?page=2")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
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

  @Test
  fun `Searching for Temporary Accommodation bookings returns 200 with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create15TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Temporary Accommodation bookings correctly filtered single booking for a specific crn`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val crn = "S121978"
        create15TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val expectedBookingSearchResult =
          createTestTemporaryAccommodationBookings(userEntity, 1, 1, crn)
        val expectedResponse = getExpectedResponseWithoutPersonName(expectedBookingSearchResult, crn)

        webTestClient.get()
          .uri("/bookings/search?crn=$crn")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Temporary Accommodation bookings correctly filtered multiple booking for a specific crn`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val crn = "S121978"
        val expectedBookingInSearchResult =
          create15TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        createTestTemporaryAccommodationBookings(userEntity, 1, 1, crn)
        val expectedResponse = getExpectedResponse(expectedBookingInSearchResult, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?crn=${offenderDetails.otherIds.crn}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Temporary Accommodation bookings with crn not exists in the database return empty response`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        create15TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val expectedBookingSearchResults = BookingSearchResults(resultsCount = 0, results = emptyList())

        webTestClient.get()
          .uri("/bookings/search?crn=S121978")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedBookingSearchResults))
      }
    }
  }

  @Test
  fun `Results are filtered by booking status when query parameter is supplied`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationRegion(userEntity.probationRegion)
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val allBeds = mutableListOf<BedEntity>()
        allPremises.forEach { premises ->
          val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }

          rooms.forEach { room ->
            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            allBeds += bed
          }
        }

        val allBookings = mutableListOf<BookingEntity>()
        allBeds.forEachIndexed { index, bed ->
          when (index % 5) {
            // Provisional
            0 -> {
              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(bed.room.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withStatus(BookingStatus.provisional)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              allBookings += booking
            }
            // Confirmed
            1 -> {
              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(bed.room.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withStatus(BookingStatus.confirmed)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val confirmation = confirmationEntityFactory.produceAndPersist {
                withBooking(booking)
              }

              booking.confirmation = confirmation
              allBookings += booking
            }
            // Active
            2 -> {
              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(bed.room.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withStatus(BookingStatus.arrived)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val arrival = arrivalEntityFactory.produceAndPersist {
                withBooking(booking)
              }

              booking.arrivals.add(arrival)
              allBookings += booking
            }
            // Closed
            3 -> {
              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(bed.room.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withStatus(BookingStatus.closed)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val departure = departureEntityFactory.produceAndPersist {
                withBooking(booking)
                withYieldedReason {
                  departureReasonEntityFactory.produceAndPersist()
                }
                withYieldedMoveOnCategory {
                  moveOnCategoryEntityFactory.produceAndPersist()
                }
              }

              booking.departures.add(departure)
              allBookings += booking
            }
            // Cancelled
            4 -> {
              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(bed.room.premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withStatus(BookingStatus.cancelled)
                withServiceName(ServiceName.temporaryAccommodation)
              }
              val cancellation = cancellationEntityFactory.produceAndPersist {
                withBooking(booking)
                withYieldedReason {
                  cancellationReasonEntityFactory.produceAndPersist()
                }
              }

              booking.cancellations.add(cancellation)
              allBookings += booking
            }
          }
        }

        val expectedBookings = allBookings.filter { it.cancellation != null }

        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?status=cancelled")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Results are ordered by the given field and sort order when the query parameters are supplied`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val expectedBookings = allBookings.sortedByDescending { it.departureDate }
        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Results are only returned for the user's probation region for Temporary Accommodation`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val expectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationRegion(userEntity.probationRegion)
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val unexpectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
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
        }

        val allPremises = expectedPremises + unexpectedPremises

        val allBeds = mutableListOf<BedEntity>()
        allPremises.forEach { premises ->
          val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }

          rooms.forEach { room ->
            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            allBeds += bed
          }
        }

        val allBookings = mutableListOf<BookingEntity>()
        allBeds.forEach { bed ->
          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(bed.room.premises)
            withCrn(offenderDetails.otherIds.crn)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          allBookings += booking
        }

        val expectedPremisesIds = expectedPremises.map { it.id }
        val expectedBookings = allBookings.filter { expectedPremisesIds.contains(it.premises.id) }

        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Temporary Accommodation bookings with pagination with pagination returns 200 with correct subset of results`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        createApprovedPremisesBookingEntities(userEntity, offenderDetails)
        val allBookings = create15TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val sortedBookings = allBookings.sortedByDescending { it.departureDate }
        val firstPage = sortedBookings.subList(0, 10)
        val secondPage = sortedBookings.subList(10, sortedBookings.size)
        val expectedFirstPageResponse = getExpectedResponse(firstPage, offenderDetails)
        val expectedSecondPageResponse = getExpectedResponse(secondPage, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=1")
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
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=2")
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

  @Test
  fun `Results are ordered by the created date and sorted descending order when the query parameters are supplied with Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val expectedBookings = allBookings.sortedByDescending { it.createdAt }
        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=createdAt&page=1")
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
  fun `Results are ordered by the created date and sorted ascending order when the query parameters are supplied with Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        allBookings.sortBy { it.createdAt }
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=ascending&sortField=createdAt&page=1")
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
  fun `Results are ordered by the start date and sorted descending order when the query parameters are supplied with Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val sortedByDescending = allBookings.sortedByDescending { it.arrivalDate }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails)
        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=startDate&page=1")
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
  fun `Results are ordered by the end date and sorted descending order when the query parameters are supplied with Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val sortedByDescending = allBookings.sortedByDescending { it.departureDate }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=1")
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
  fun `Results are ordered by the person crn and sorted descending order when the query parameters are supplied with Pagination`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val sortedByDescending = allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=crn&page=1&status=provisional")
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
        val allBookings = create15TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        val sortedByDescending = allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=crn&status=provisional")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `No Results returned when searching for cancelled booking status and all existing bookings are confirmed`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails)
        allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(emptyList(), offenderDetails)

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=crn&page=1&status=cancelled")
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

  private fun getExpectedResponse(expectedBookings: List<BookingEntity>, offenderDetails: OffenderDetailSummary): BookingSearchResults {
    return BookingSearchResults(
      resultsCount = expectedBookings.size,
      results = expectedBookings.map { booking ->
        BookingSearchResult(
          person = BookingSearchResultPersonSummary(
            name = "${offenderDetails.firstName} ${offenderDetails.surname}",
            crn = offenderDetails.otherIds.crn,
          ),
          booking = BookingSearchResultBookingSummary(
            id = booking.id,
            status = when {
              booking.cancellation != null -> BookingStatus.cancelled
              booking.departure != null -> BookingStatus.departed
              booking.arrival != null -> BookingStatus.arrived
              booking.nonArrival != null -> BookingStatus.notMinusArrived
              booking.confirmation != null -> BookingStatus.confirmed
              booking.service == ServiceName.approvedPremises.value -> BookingStatus.awaitingMinusArrival
              else -> BookingStatus.provisional
            },
            startDate = booking.arrivalDate,
            endDate = booking.departureDate,
            createdAt = booking.createdAt.toInstant(),
          ),
          premises = BookingSearchResultPremisesSummary(
            id = booking.premises.id,
            name = booking.premises.name,
            addressLine1 = booking.premises.addressLine1,
            addressLine2 = booking.premises.addressLine2,
            town = booking.premises.town,
            postcode = booking.premises.postcode,
          ),
          room = BookingSearchResultRoomSummary(
            id = booking.bed!!.room.id,
            name = booking.bed!!.room.name,
          ),
          bed = BookingSearchResultBedSummary(
            id = booking.bed!!.id,
            name = booking.bed!!.name,
          ),
        )
      },
    )
  }

  private fun getExpectedResponseWithoutPersonName(expectedBookings: List<BookingEntity>, crn: String): BookingSearchResults {
    return BookingSearchResults(
      resultsCount = expectedBookings.size,
      results = expectedBookings.map { booking ->
        BookingSearchResult(
          person = BookingSearchResultPersonSummary(
            name = null,
            crn = crn,
          ),
          booking = BookingSearchResultBookingSummary(
            id = booking.id,
            status = when {
              booking.cancellation != null -> BookingStatus.cancelled
              booking.departure != null -> BookingStatus.departed
              booking.arrival != null -> BookingStatus.arrived
              booking.nonArrival != null -> BookingStatus.notMinusArrived
              booking.confirmation != null -> BookingStatus.confirmed
              booking.service == ServiceName.approvedPremises.value -> BookingStatus.awaitingMinusArrival
              else -> BookingStatus.provisional
            },
            startDate = booking.arrivalDate,
            endDate = booking.departureDate,
            createdAt = booking.createdAt.toInstant(),
          ),
          premises = BookingSearchResultPremisesSummary(
            id = booking.premises.id,
            name = booking.premises.name,
            addressLine1 = booking.premises.addressLine1,
            addressLine2 = booking.premises.addressLine2,
            town = booking.premises.town,
            postcode = booking.premises.postcode,
          ),
          room = BookingSearchResultRoomSummary(
            id = booking.bed!!.room.id,
            name = booking.bed!!.room.name,
          ),
          bed = BookingSearchResultBedSummary(
            id = booking.bed!!.id,
            name = booking.bed!!.name,
          ),
        )
      },
    )
  }

  private fun create10TestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): MutableList<BookingEntity> {
    return createTestTemporaryAccommodationBookings(userEntity, offenderDetails, 5, 2)
  }

  private fun create15TestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): MutableList<BookingEntity> {
    return createTestTemporaryAccommodationBookings(userEntity, offenderDetails, 5, 3)
  }

  private fun createTestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
    numberOfPremises: Int,
    numberOfBedsInEachPremises: Int,
  ): MutableList<BookingEntity> {
    return createTestTemporaryAccommodationBookings(userEntity, numberOfPremises, numberOfBedsInEachPremises, offenderDetails.otherIds.crn)
  }

  private fun createTestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    numberOfPremises: Int,
    numberOfBedsInEachPremises: Int,
    crn: String,
  ): MutableList<BookingEntity> {
    val allPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(numberOfPremises) {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val allBeds = mutableListOf<BedEntity>()
    allPremises.forEach { premises ->
      val rooms = roomEntityFactory.produceAndPersistMultiple(numberOfBedsInEachPremises) {
        withPremises(premises)
      }

      rooms.forEach { room ->
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        allBeds += bed
      }
    }

    val allBookings = mutableListOf<BookingEntity>()
    allBeds.forEachIndexed { index, bed ->
      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(bed.room.premises)
        withCrn(crn)
        withBed(bed)
        withStatus(BookingStatus.provisional)
        withServiceName(ServiceName.temporaryAccommodation)
        withArrivalDate(LocalDate.now().minusDays((60 - index).toLong()))
        withDepartureDate(LocalDate.now().minusDays((30 - index).toLong()))
        withCreatedAt(LocalDate.now().minusDays((30 - index).toLong()).toLocalDateTime())
      }

      allBookings += booking
    }
    return allBookings
  }

  private fun createApprovedPremisesBookingEntities(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): MutableList<BookingEntity> {
    val allPremises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val allBeds = mutableListOf<BedEntity>()
    allPremises.forEach { premises ->
      val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
        withPremises(premises)
      }

      rooms.forEach { room ->
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        allBeds += bed
      }
    }

    val allBookings = mutableListOf<BookingEntity>()
    allBeds.forEach { bed ->
      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(bed.room.premises)
        withCrn(offenderDetails.otherIds.crn)
        withBed(bed)
        withServiceName(ServiceName.approvedPremises)
      }

      allBookings += booking
    }
    return allBookings
  }
}
