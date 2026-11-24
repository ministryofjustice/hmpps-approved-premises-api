package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class Cas1TasksDeallocateTest : IntegrationTestBase() {
  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER"], mode = EnumSource.Mode.EXCLUDE)
  fun `Return 403 forbidden for users with roles other than CAS1_CRU_MEMBER`(userRole: UserRole) {
    val (_, jwt) = givenAUser(roles = listOf(userRole))
    webTestClient.delete()
      .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Deallocate assessment without JWT returns 401 Unauthorized`() {
    webTestClient.delete()
      .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Deallocate Approved Premises assessment returns 403 Forbidden`() {
    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        givenAUser { _, _ ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { assessment, _ ->
            webTestClient.delete()
              .uri("/cas1/tasks/assessment/${assessment.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }
  }
}
