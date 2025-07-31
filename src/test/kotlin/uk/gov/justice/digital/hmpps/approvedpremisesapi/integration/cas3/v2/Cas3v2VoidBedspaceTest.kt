package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2VoidBedspaceTest : Cas3IntegrationTestBase() {

  @Nested
  inner class GetVoidBedspaces {

    @Test
    fun `user without CAS_ASSESSOR role is forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        doGetRequest(jwt, premises.id)
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `get void bedspaces returns successfully`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaces = createVoidBedspaces(premises)
        val cancelledVoidBedspaces = createCancelledVoidBedspaces(premises)

        val result = doGetRequest(jwt, premises.id)
          .expectStatus()
          .isOk
          .expectBodyList(Cas3VoidBedspace::class.java)
          .returnResult()
          .responseBody!!

        assertAll({
          assertThat(result).hasSize(3)
          assertThat(result.map { it.id }).containsExactlyInAnyOrderElementsOf(voidBedspaces.map { it.id })
          assertThat(result.map { it.id }).doesNotContainAnyElementsOf(cancelledVoidBedspaces.map { it.id })
        })
      }
    }

    fun doGetRequest(jwt: String, premisesId: UUID) = webTestClient.get()
      .uri("/cas3/v2/premises/$premisesId/void-bedspaces")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
      .exchange()

    fun createVoidBedspaces(premises: Cas3PremisesEntity): List<Cas3VoidBedspaceEntity> {
      val bedspaces = cas3BedspaceEntityFactory.produceAndPersistMultiple(5) {
        withPremises(premises)
      }
      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
      val voidBedspace1 = cas3VoidBedspaceEntityFactory.produceAndPersist {
        withBedspace(bedspaces.get(0))
        withYieldedReason { reason }
      }
      val voidBedspace2 = cas3VoidBedspaceEntityFactory.produceAndPersist {
        withBedspace(bedspaces.get(3))
        withYieldedReason { reason }
      }
      val voidBedspace3 = cas3VoidBedspaceEntityFactory.produceAndPersist {
        withBedspace(bedspaces.get(4))
        withYieldedReason { reason }
      }

      return listOf(voidBedspace1, voidBedspace2, voidBedspace3)
    }

    fun createCancelledVoidBedspaces(premises: Cas3PremisesEntity): List<Cas3VoidBedspaceEntity> {
      val bedspaces = cas3BedspaceEntityFactory.produceAndPersistMultiple(2) {
        withPremises(premises)
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
      val voidBedspace1 = cas3VoidBedspaceEntityFactory.produceAndPersist {
        withBedspace(bedspaces.get(0))
        withYieldedReason { reason }
        withCancellationDate(OffsetDateTime.now())
      }

      val voidBedspace2 = cas3VoidBedspaceEntityFactory.produceAndPersist {
        withBedspace(bedspaces.get(1))
        withYieldedReason { reason }
        withCancellationDate(OffsetDateTime.now())
        withCancellationNotes("Cancelled")
      }

      return listOf(voidBedspace1, voidBedspace2)
    }
  }
}
