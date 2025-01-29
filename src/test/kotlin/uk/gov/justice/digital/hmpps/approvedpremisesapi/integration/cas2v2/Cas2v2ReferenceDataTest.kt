package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2v2

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2v2PersistedApplicationStatusFinder

class Cas2v2ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var statusFinder: Cas2v2PersistedApplicationStatusFinder

  @Test
  fun `All available application status options are returned`() {
    val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt()

    val response = webTestClient.get()
      .uri("/cas2v2/reference-data/application-status")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .returnResult()
      .responseBodyContent

    val options =
      objectMapper.readValue(response, object : TypeReference<List<Cas2v2ApplicationStatus>>() {})

    assert(options.size == 9)

    val moreInfoRequestedStatus = statusFinder.findStatusByName("moreInfoRequested")
    val riskToSelfDetails = statusFinder.findDetailsBy(moreInfoRequestedStatus!!.id) { it.name == "riskToSelf" }
    assertThat(riskToSelfDetails).isNotNull

    // Make sure FIE is in the options returned, and this contains riskToSelf within it
    val fie = options.find { option -> option.name == moreInfoRequestedStatus.name }
    assertThat(fie).isNotNull
    assertThat(fie!!.statusDetails[3].id).isEqualTo(riskToSelfDetails!!.id)
    assertThat(fie.statusDetails[3].children?.size).isEqualTo(riskToSelfDetails.children?.size)
  }
}
