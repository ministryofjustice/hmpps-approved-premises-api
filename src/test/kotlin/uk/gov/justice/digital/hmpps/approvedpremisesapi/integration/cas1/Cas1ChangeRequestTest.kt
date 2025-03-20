package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import java.time.LocalDate
import java.util.UUID

class Cas1ChangeRequestTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var cas1ChangeRequestRepository: Cas1ChangeRequestRepository

  @Nested
  inner class CreateChangeRequest {

    @Test
    fun `Without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/placement-request/${UUID.randomUUID()}/change-request")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Returns 403 when type is appeal and role is invalid`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/change-request")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.APPEAL,
                requestJson = "{}",
                reasonId = UUID.randomUUID(),
                spaceBookingId = UUID.randomUUID(),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Returns 403 when type is planned transfer and role is invalid`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/change-request")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLANNED_TRANSFER,
                requestJson = "{}",
                reasonId = UUID.randomUUID(),
                spaceBookingId = UUID.randomUUID(),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Returns 400 when type is extension`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/change-request")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.EXTENSION,
                requestJson = "{}",
                reasonId = UUID.randomUUID(),
                spaceBookingId = UUID.randomUUID(),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `Returns 200 when post new appeal change request is successful`() {
      givenAUser(roles = listOf(UserRole.CAS1_CHANGE_REQUEST_DEV)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          val spaceBooking = givenACas1SpaceBooking(
            crn = placementRequest.application.crn,
            placementRequest = placementRequest,
          )
          val changeRequestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist()
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/change-request")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.APPEAL,
                requestJson = "{test: 1}",
                reasonId = changeRequestReason.id,
                spaceBookingId = spaceBooking.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedChangeRequest = cas1ChangeRequestRepository.findAll()[0]
          assertThat(persistedChangeRequest.type).isEqualTo(ChangeRequestType.APPEAL)
          assertThat(persistedChangeRequest.requestJson).isEqualTo("\"{test: 1}\"")
          assertThat(persistedChangeRequest.requestReason).isEqualTo(changeRequestReason)
          assertThat(persistedChangeRequest.spaceBooking.id).isEqualTo(spaceBooking.id)
        }
      }
    }

    @Test
    fun `Returns 200 when post new planned transfer change request is successful`() {
      givenAUser(roles = listOf(UserRole.CAS1_CHANGE_REQUEST_DEV)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          val spaceBooking = givenACas1SpaceBooking(
            crn = placementRequest.application.crn,
            placementRequest = placementRequest,
            actualArrivalDate = LocalDate.now(),
          )
          val changeRequestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist {
            withChangeRequestType(ChangeRequestType.PLANNED_TRANSFER)
          }
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/change-request")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLANNED_TRANSFER,
                requestJson = "{test: 1}",
                reasonId = changeRequestReason.id,
                spaceBookingId = spaceBooking.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedChangeRequest = cas1ChangeRequestRepository.findAll()[0]
          assertThat(persistedChangeRequest.type).isEqualTo(ChangeRequestType.PLANNED_TRANSFER)
          assertThat(persistedChangeRequest.requestJson).isEqualTo("\"{test: 1}\"")
          assertThat(persistedChangeRequest.requestReason).isEqualTo(changeRequestReason)
          assertThat(persistedChangeRequest.spaceBooking.id).isEqualTo(spaceBooking.id)
        }
      }
    }
  }
}
