package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

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
                withYieldedProbationRegion { probationRegion }
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
}
