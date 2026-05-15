package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.entity.Cas1FormDataEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.entity.Cas1FormDataRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals

class Cas1FormDataTest {

  @Nested
  @DisplayName("GET /cas1/form-data/{id}")
  inner class GetFormData : IntegrationTestBase() {

    @Autowired
    private lateinit var cas1FormDataRepository: Cas1FormDataRepository

    @Test
    fun `Without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/form-data/my-key")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `404 when form data could not be found`() {
      val (_, jwt) = givenAUser()

      webTestClient.get()
        .uri("/cas1/form-data/my-key")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Retrieve existing form data`() {
      cas1FormDataRepository.save(Cas1FormDataEntity("my-key", """{ "key": "value"}"""))

      val (_, jwt) = givenAUser()

      webTestClient.get()
        .uri("/cas1/form-data/my-key")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("""{ "key": "value"}""")
    }
  }

  @Nested
  @DisplayName("PUT /cas1/form-data/{id}")
  inner class Update : IntegrationTestBase() {

    @Autowired
    private lateinit var cas1FormDataRepository: Cas1FormDataRepository

    @Test
    fun `Without JWT returns 401`() {
      webTestClient.put()
        .uri("/cas1/form-data/my-key")
        .bodyValue("""{ "key": "value"}""")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Add new form data`() {
      val (_, jwt) = givenAUser()

      webTestClient.put()
        .uri("/cas1/form-data/my-key")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("""{ "key": "value"}""")
        .exchange()
        .expectStatus().isOk

      assertJsonEquals(
        cas1FormDataRepository.findByIdOrNull("my-key")!!.value,
        """{ "key": "value"}""",
      )
    }

    @Test
    fun `Update existing form data`() {
      cas1FormDataRepository.save(Cas1FormDataEntity("my-key", """{ "key": "value"}"""))

      val (_, jwt) = givenAUser()

      webTestClient.put()
        .uri("/cas1/form-data/my-key")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("""{ "new-key": "new-value"}""")
        .exchange()
        .expectStatus().isOk

      assertJsonEquals(
        cas1FormDataRepository.findByIdOrNull("my-key")!!.value,
        """{ "new-key": "new-value"}""",
      )
    }
  }
}
