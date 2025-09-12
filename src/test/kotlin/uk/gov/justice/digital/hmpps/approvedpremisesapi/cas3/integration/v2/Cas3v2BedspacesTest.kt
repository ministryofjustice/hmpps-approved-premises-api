package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3v2BedspacesTest : Cas3IntegrationTestBase() {

  @Nested
  inner class CreateBedspace {
    @Test
    fun `Create new bedspace for Premises returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val bedspaceCharacteristics = cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(5)
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = bedspaceCharacteristics.map { it.id },
          notes = randomStringLowerCase(100),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newBedspace.reference)
          .jsonPath("startDate").isEqualTo(newBedspace.startDate.toString())
          .jsonPath("notes").isEqualTo(newBedspace.notes.toString())
          .jsonPath("bedspaceCharacteristics[*].id").isEqualTo(bedspaceCharacteristics.map { it.id.toString() })
          .jsonPath("bedspaceCharacteristics[*].name").isEqualTo(bedspaceCharacteristics.map { it.name })
      }
    }

    @Test
    fun `Create new bedspace in a scheduled to archive premises returns 201 Created with correct body and unarchive the premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.archived,
          endDate = LocalDate.now().plusDays(3),
        )
        val bedspaceCharacteristics = cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(5)
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now().minusDays(2),
          characteristicIds = bedspaceCharacteristics.map { it.id },
          notes = randomStringLowerCase(100),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newBedspace.reference)
          .jsonPath("startDate").isEqualTo(LocalDate.now())
          .jsonPath("notes").isEqualTo(newBedspace.notes.toString())
          .jsonPath("bedspaceCharacteristics[*].id").isEqualTo(bedspaceCharacteristics.map { it.id.toString() })
          .jsonPath("bedspaceCharacteristics[*].name").isEqualTo(bedspaceCharacteristics.map { it.name })

        // verify premises is unarchived
        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(updatedPremises.startDate).isEqualTo(newBedspace.startDate)
        assertThat(updatedPremises.endDate).isNull()

        val premisesUnarchiveDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesUnarchiveDomainEvents).isNotNull()
        assertThat(premisesUnarchiveDomainEvents.size).isEqualTo(1)
      }
    }

    @Test
    fun `When a new bedspace is created with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("notes").isEmpty()
      }
    }

    @Test
    fun `When create a new bedspace without a reference returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = "",
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.reference")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `When create a new bedspace with start date before premises start date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = "",
          startDate = premises.startDate.minusDays(3),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.startDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("startDateBeforePremisesStartDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(premises.startDate.toString())
      }
    }

    @Test
    fun `When create a new bedspace with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(7),
          startDate = LocalDate.now(),
          characteristicIds = mutableListOf(UUID.randomUUID()),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Create new bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = givenACas3Premises()
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }
}
