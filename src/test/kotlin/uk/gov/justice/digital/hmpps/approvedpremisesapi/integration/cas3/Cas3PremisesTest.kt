package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var cas3FutureBookingTransformer: Cas3FutureBookingTransformer

  @Nested
  inner class GetFutureBookings {
    @Test
    fun `Get future bookings without JWT returns 401`() {
      webTestClient.get()
        .uri("cas3/premises/${UUID.randomUUID()}/future-bookings")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get future bookings for premises out of user region returns 403`() {
      givenAUser { user, jwt ->
        val probationRegion = probationRegionEntityFactory.produceAndPersist()
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
        }

        webTestClient.get()
          .uri("cas3/premises/${premises.id}/future-bookings?statuses=provisional")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get future bookings for non existing premises returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premisesId = UUID.randomUUID()

        webTestClient.get()
          .uri("cas3/premises/$premisesId/future-bookings?statuses=provisional")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
      }
    }

    @Test
    fun `Get future bookings returns OK with empty booking list if there are no future bookings`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().minusDays(60))
          withDepartureDate(LocalDate.now().minusDays(3))
        }

        webTestClient.get()
          .uri("cas3/premises/${premises.id}/future-bookings?statuses=provisional")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf<FutureBooking>(),
            ),
          )
      }
    }

    @Test
    fun `Get future bookings returns OK with expected future bookings`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
        }

        val offenderCaseSummary = CaseSummaryFactory()
          .withCurrentExclusion(false)
          .withCurrentRestriction(false)
          .produce()

        // future provisional booking
        val provisionalBooking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderCaseSummary.crn)
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().plusDays(1))
          withDepartureDate(LocalDate.now().plusDays(60))
        }

        // future confirmed booking
        val confirmedBooking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderCaseSummary.crn)
          withStatus(BookingStatus.confirmed)
          withArrivalDate(LocalDate.now().plusDays(10))
          withDepartureDate(LocalDate.now().plusDays(43))
        }

        confirmedBooking.confirmation = confirmationEntityFactory.produceAndPersist {
          withBooking(confirmedBooking)
        }

        // arrived booking with future departure date
        val arrivedBooking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderCaseSummary.crn)
          withStatus(BookingStatus.arrived)
          withArrivalDate(LocalDate.now().minusDays(10))
          withDepartureDate(LocalDate.now().plusDays(22))
        }

        arrivedBooking.arrivals = listOf(
          arrivalEntityFactory.produceAndPersist {
            withBooking(arrivedBooking)
            withArrivalDate(LocalDate.now().minusDays(10))
          },
        ).toMutableList()

        // provisional booking in the past
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().minusDays(19))
          withDepartureDate(LocalDate.now().minusDays(7))
        }

        // cancelled booking
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.cancelled)
          withArrivalDate(LocalDate.now().plusDays(7))
          withDepartureDate(LocalDate.now().plusDays(72))
        }

        // departed booking in the past
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.departed)
          withArrivalDate(LocalDate.now().minusDays(30))
          withDepartureDate(LocalDate.now())
        }

        // confirmed booking in the past
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.confirmed)
          withArrivalDate(LocalDate.now().minusDays(40))
          withDepartureDate(LocalDate.now().minusDays(10))
        }

        val expectedJson = objectMapper.writeValueAsString(
          listOf(provisionalBooking, confirmedBooking, arrivedBooking).map {
            cas3FutureBookingTransformer.transformJpaToApi(
              it,
              PersonSummaryInfoResult.Success.Full(it.crn, offenderCaseSummary),
            )
          },
        )

        apDeliusContextAddCaseSummaryToBulkResponse(offenderCaseSummary)

        apDeliusContextAddResponseToUserAccessCall(
          listOf(
            CaseAccessFactory()
              .withCrn(offenderCaseSummary.crn)
              .produce(),
          ),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("cas3/premises/${premises.id}/future-bookings?statuses=provisional,confirmed,arrived")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }
}
