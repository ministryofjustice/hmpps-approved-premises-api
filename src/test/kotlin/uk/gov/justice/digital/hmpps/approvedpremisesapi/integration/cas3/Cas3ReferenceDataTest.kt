package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3DutyToReferOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3DutyToReferOutcomeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferOutcomeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferOutcomeRepository
import java.util.UUID

class Cas3ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas3DutyToReferOutcomeRepository: Cas3DutyToReferOutcomeRepository

  lateinit var cas3DutyToReferOutcomeEntityFactory: PersistedFactory<Cas3DutyToReferOutcomeEntity, UUID, Cas3DutyToReferOutcomeEntityFactory>

  @BeforeEach
  fun setup() {
    cas3DutyToReferOutcomeEntityFactory = PersistedFactory({ Cas3DutyToReferOutcomeEntityFactory() }, cas3DutyToReferOutcomeRepository)
    cas3DutyToReferOutcomeRepository.deleteAll()
  }

  @Test
  fun `Get duty to refer outcome returns 200 with correct active duty to referral outcomes`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
    val cas3DutyToReferOutcome = cas3DutyToReferOutcomeEntityFactory.produceAndPersistMultiple(5)
    val expectedJson = objectMapper.writeValueAsString(
      cas3DutyToReferOutcome.map { e -> Cas3DutyToReferOutcome(e.id, e.name, e.isActive) },
    )

    webTestClient.get()
      .uri("cas3/reference-data/duty-to-refer-outcomes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get duty to refer outcome returns 200 with correct active duty to referral outcome when inactive outcome presents`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
    cas3DutyToReferOutcomeEntityFactory.produceAndPersist {
      withIsActive(false)
    }
    val cas3DutyToReferOutcome = cas3DutyToReferOutcomeEntityFactory.produceAndPersistMultiple(5)
    val expectedJson = objectMapper.writeValueAsString(
      cas3DutyToReferOutcome.map { e -> Cas3DutyToReferOutcome(e.id, e.name, e.isActive) },
    )

    webTestClient.get()
      .uri("cas3/reference-data/duty-to-refer-outcomes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
