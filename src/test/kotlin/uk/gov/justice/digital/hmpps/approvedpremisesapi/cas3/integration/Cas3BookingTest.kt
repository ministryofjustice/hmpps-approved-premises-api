package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3BookingTest : IntegrationTestBase() {
  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class CreateDeparture {
    @Test
    fun `Create Departure on Temporary Accommodation Booking when a departure already exists returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val departureDate = Instant.now()
          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises {
              temporaryAccommodationPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion { userEntity.probationRegion }
              }
            }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.now().minusDays(60))
            withDepartureDate(LocalDate.now().minusDays(5))
            withCreatedAt(OffsetDateTime.now().minusDays(60))
          }

          val reason = departureReasonEntityFactory.produceAndPersist {
            withServiceScope("temporary-accommodation")
          }
          val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
            withServiceScope("temporary-accommodation")
          }

          val departure = departureEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(reason)
            withMoveOnCategory(moveOnCategory)
          }
          booking.departures = mutableListOf(departure)

          webTestClient.post()
            .uri("/cas3/premises/${booking.premises.id}/bookings/${booking.id}/departures")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas3NewDeparture(
                dateTime = departureDate,
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                notes = "Corrected date",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.dateTime").isEqualTo(departureDate)
            .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.moveOnCategory.id").isEqualTo(moveOnCategory.id.toString())
            .jsonPath("$.notes").isEqualTo("Corrected date")
            .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
        }
      }
    }
  }

  @Nested
  inner class CreateArrival {
    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival without JWT returns 401`(url: String) {
      webTestClient.post()
        .uri("$url/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/arrivals")
        .bodyValue(
          NewCas3Arrival(
            type = "CAS3",
            arrivalDate = LocalDate.parse("2022-08-12"),
            expectedDepartureDate = LocalDate.parse("2022-08-14"),
            notes = null,
            keyWorkerStaffCode = "123",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival returns 409 Conflict when another booking for the same bed overlaps with the arrival and expected departure dates`(url: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withName("test-bed")
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withName("test-room")
                withYieldedPremises { premises }
              }
            }
          }

          val conflictingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("$url/${premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = LocalDate.parse("2022-06-16"),
                expectedDepartureDate = LocalDate.parse("2022-07-16"),
                notes = "Moved in late due to sickness",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail")
            .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival returns 409 Conflict when a void bedspace for the same bed overlaps with the arrival and expected departure dates`(url: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withName("test-bed")
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withName("test-room")
                withYieldedPremises { premises }
              }
            }
          }

          val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          webTestClient.post()
            .uri("$url/${premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = LocalDate.parse("2022-06-16"),
                expectedDepartureDate = LocalDate.parse("2022-07-16"),
                notes = "Moved in late due to sickness",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail")
            .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival updates arrival and departure date for a booking`(url: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { userEntity.probationRegion }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val arrivalDate = LocalDate.now().minusDays(1)
          val expectedDepartureDate = arrivalDate.plusDays(2)

          webTestClient.post()
            .uri("$url/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = arrivalDate,
                expectedDepartureDate = expectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          webTestClient.get()
            .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo(arrivalDate)
            .jsonPath("$.departureDate").isEqualTo(expectedDepartureDate)
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival and departure date for a booking and emit arrival domain event for new arrival`(url: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = givenATemporaryAccommodationPremises(region = userEntity.probationRegion)

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val arrivalDate = LocalDate.now().minusDays(1)
          val expectedDepartureDate = arrivalDate.plusDays(2)

          webTestClient.post()
            .uri("$url/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = arrivalDate,
                expectedDepartureDate = expectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          webTestClient.get()
            .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo(arrivalDate)
            .jsonPath("$.departureDate").isEqualTo(expectedDepartureDate)
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")

          assertPublishedSNSEvent(
            booking,
            DomainEventType.CAS3_PERSON_ARRIVED,
            "Someone has arrived at a Transitional Accommodation premises for their booking",
            "http://api/events/cas3/person-arrived",
          )
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival returns field validation error when the arrival date is more than 14 days in the past CAS3`(url: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = givenATemporaryAccommodationPremises(region = userEntity.probationRegion)

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val arrivalDate = LocalDate.now().minusDays(15)
          val expectedDepartureDate = arrivalDate.plusDays(2)

          webTestClient.post()
            .uri("$url/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = arrivalDate,
                expectedDepartureDate = expectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.arrivalDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("arrivalAfterLatestDate")
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival updates arrival for a booking with existing arrival and updated domain event send`(url: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { userEntity.probationRegion }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }
          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }
          booking.let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(2) { withBooking(it) }.toMutableList()
          }

          val arrivalDate = LocalDate.now().minusDays(1)
          val expectedDepartureDate = arrivalDate.plusDays(2)

          webTestClient.post()
            .uri("$url/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = arrivalDate,
                expectedDepartureDate = expectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          webTestClient.get()
            .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo(arrivalDate)
            .jsonPath("$.departureDate").isEqualTo(expectedDepartureDate)
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")

          assertPublishedSNSEvent(
            booking,
            DomainEventType.CAS3_PERSON_ARRIVED_UPDATED,
            "Someone has changed arrival date at a Transitional Accommodation premises for their booking",
            "http://api/events/cas3/person-arrived-updated",
          )
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival for a booking on a premises that's not in the user's region returns 403 Forbidden`(url: String) {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { userEntity.probationRegion }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.post()
            .uri("$url/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = LocalDate.parse("2022-08-12"),
                expectedDepartureDate = LocalDate.parse("2022-08-14"),
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "/cas3/premises")
    fun `Create Arrival for a booking on a premises that does not exist returns 404 Not Found`(url: String) {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { userEntity.probationRegion }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val premisesId = UUID.randomUUID()

          webTestClient.post()
            .uri("$url/$premisesId/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = LocalDate.parse("2022-08-12"),
                expectedDepartureDate = LocalDate.parse("2022-08-14"),
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
        }
      }
    }

    private fun assertPublishedSNSEvent(
      booking: BookingEntity,
      eventType: DomainEventType,
      eventDescription: String,
      detailUrl: String,
    ) {
      val emittedMessage = snsDomainEventListener.blockForMessage(eventType)

      assertThat(emittedMessage.description).isEqualTo(eventDescription)
      assertThat(emittedMessage.detailUrl).matches("$detailUrl/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
      assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
        SnsEventPersonReference("CRN", booking.crn),
        SnsEventPersonReference("NOMS", booking.nomsNumber!!),
      )
    }
  }
}
