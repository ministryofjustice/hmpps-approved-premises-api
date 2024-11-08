package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyBookingEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyEntryType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.LocalDate

class CalendarTest : InitialiseDatabasePerClassTestBase() {
  @Test
  fun `Requesting Calendar without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/52a7af24-2e6c-4276-8010-e1b88f87ff46/calendar?startDate=2023-06-12&endDate=2023-07-12")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Requesting Calendar for CAS1 Premises without CAS1_MATCHER or CAS1_MANAGER role returns 403`() {
    givenAUser { _, jwt ->
      givenAnApprovedPremisesBed { bed ->
        webTestClient.get()
          .uri("/premises/${bed.room.premises.id}/calendar?startDate=2023-06-08&endDate=2023-07-10")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `Requesting Calendar for CAS1 Premises with CAS1_MATCHER or CAS1_MANAGER returns 200 with correct body`(role: UserRole) {
    givenAUser(
      roles = listOf(role),
    ) { _, jwt ->
      givenAnApprovedPremisesBed { bed ->
        givenAnOffender { offenderDetails, _ ->
          val booking = bookingEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(bed.room.premises)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 6, 8))
            withDepartureDate(LocalDate.of(2023, 6, 10))
          }

          webTestClient.get()
            .uri("/premises/${bed.room.premises.id}/calendar?startDate=2023-06-08&endDate=2023-06-10")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  BedOccupancyRange(
                    bedId = bed.id,
                    bedName = bed.name,
                    schedule = listOf(
                      BedOccupancyBookingEntry(
                        bookingId = booking.id,
                        personName = "${offenderDetails.firstName} ${offenderDetails.surname}",
                        type = BedOccupancyEntryType.booking,
                        length = 3,
                        startDate = LocalDate.of(2023, 6, 8),
                        endDate = LocalDate.of(2023, 6, 10),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }
  }
}
