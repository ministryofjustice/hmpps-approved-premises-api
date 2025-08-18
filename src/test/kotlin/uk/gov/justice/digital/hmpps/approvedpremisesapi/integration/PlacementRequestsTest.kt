package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class PlacementRequestsTest : IntegrationTestBase() {

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class CreateBookingNotMadeFromPlacementRequest {
    @Test
    fun `Create a Booking Not Made from a Placement Request without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking-not-made")
        .bodyValue(
          NewBookingNotMade(
            notes = "some notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER"], mode = EnumSource.Mode.INCLUDE)
    fun `Create a Booking Not Made from a Placement Request returns 200 and creates a domain event`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = otherUser) {
              givenAPlacementRequest(
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking-not-made")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewBookingNotMade(
                      notes = "some notes",
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .jsonPath("$.placementRequestId").isEqualTo(placementRequest.id.toString())
                  .jsonPath("$.notes").isEqualTo("some notes")

                domainEventAsserter.assertDomainEventOfTypeStored(
                  placementRequest.application.id,
                  DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
                )
              }
            }
          }
        }
      }
    }
  }
}
