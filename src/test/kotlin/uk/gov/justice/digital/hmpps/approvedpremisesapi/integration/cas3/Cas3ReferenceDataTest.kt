package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3DutyToReferRejectionReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3DutyToReferRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferRejectionReasonRepository
import java.util.UUID

class Cas3ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas3DutyToReferRejectionReasonRepository: Cas3DutyToReferRejectionReasonRepository

  lateinit var cas3DutyToReferRejectionReasonEntityFactory: PersistedFactory<Cas3DutyToReferRejectionReasonEntity, UUID, Cas3DutyToReferRejectionReasonEntityFactory>

  @BeforeEach
  fun setup() {
    cas3DutyToReferRejectionReasonEntityFactory = PersistedFactory({ Cas3DutyToReferRejectionReasonEntityFactory() }, cas3DutyToReferRejectionReasonRepository)
    cas3DutyToReferRejectionReasonRepository.deleteAll()
  }

  @Test
  fun `Get duty to refer rejection reason returns 200 with correct active duty to referral rejection reasons`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
    val cas3DutyToReferRejectionReasons = cas3DutyToReferRejectionReasonEntityFactory.produceAndPersistMultiple(5)
    val expectedJson = objectMapper.writeValueAsString(
      cas3DutyToReferRejectionReasons.map { e -> Cas3DutyToReferRejectionReason(e.id, e.name, e.isActive) },
    )

    webTestClient.get()
      .uri("cas3/reference-data/duty-to-refer-rejection-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get duty to refer rejection reason returns 200 with correct active duty to referral rejection reasons when inactive reason presents`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
    cas3DutyToReferRejectionReasonEntityFactory.produceAndPersist {
      withIsActive(false)
    }
    val cas3DutyToReferRejectionReasons = cas3DutyToReferRejectionReasonEntityFactory.produceAndPersistMultiple(5)
    val expectedJson = objectMapper.writeValueAsString(
      cas3DutyToReferRejectionReasons.map { e -> Cas3DutyToReferRejectionReason(e.id, e.name, e.isActive) },
    )

    webTestClient.get()
      .uri("cas3/reference-data/duty-to-refer-rejection-reasons")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
