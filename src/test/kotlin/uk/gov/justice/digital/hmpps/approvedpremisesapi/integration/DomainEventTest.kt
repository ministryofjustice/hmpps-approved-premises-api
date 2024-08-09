package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.createCas1DomainEventEnvelopeOfType
import java.util.UUID

class DomainEventTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var domainEventUrlConfig: DomainEventUrlConfig

  private fun generateUrlForDomainEventType(domainEventType: DomainEventType, uuid: UUID) = domainEventUrlConfig.getUrlForDomainEventId(domainEventType, uuid)
    .replace("http://api", "")

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `Get event without JWT returns 401`(domainEventType: DomainEventType) {
    val url = generateUrlForDomainEventType(domainEventType, UUID.randomUUID())

    webTestClient.get()
      .uri(url)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `Get event without ROLE_APPROVED_PREMISES_EVENTS returns 403`(domainEventType: DomainEventType) {
    val url = generateUrlForDomainEventType(domainEventType, UUID.randomUUID())
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  companion object {
    @JvmStatic
    fun allCas1DomainEventTypes() = DomainEventType.values().filter { it.cas == DomainEventCas.CAS1 }
  }

  @ParameterizedTest
  @MethodSource("allCas1DomainEventTypes")
  fun `Get event returns 200 with correct body`(domainEventType: DomainEventType) {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val envelopedData = createCas1DomainEventEnvelopeOfType(domainEventType)

    val event = domainEventFactory.produceAndPersist {
      withType(domainEventType)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val url = generateUrlForDomainEventType(domainEventType, event.id)

    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(envelopedData::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }
}
