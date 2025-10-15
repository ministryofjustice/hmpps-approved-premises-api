package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2LicenceCaseAdminUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender

class Cas2ApplicationAbandonTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realApplicationRepository: Cas2ApplicationRepository

  val schema = """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """

  val data = """
          {
             "thingId": 123
          }
          """

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `abandoning an application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "cas2",
        roles = listOf(role),
      )

      webTestClient.put()
        .uri("/cas2/applications/66911cf0-75b1-4361-84bd-501b176fd4fd/abandon")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Abandon application without JWT returns 401`() {
      webTestClient.put()
        .uri("/cas2/applications/9b785e59-b85c-4be0-b271-d9ac287684b6/abandon")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class PutToAbandon {

    @Nested
    inner class PomUsers {
      @Test
      fun `Abandon existing CAS2 application returns 200 with correct body`() {
        givenACas2PomUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, submittingUser)

            webTestClient.put()
              .uri("/cas2/applications/${application.id}/abandon")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk

            Assertions.assertNotNull(realApplicationRepository.findByIdAndServiceOrigin(application.id, Cas2ServiceOrigin.HDC)?.abandonedAt)
          }
        }
      }
    }

    @Nested
    inner class LicenceCaseAdminUsers {
      @Test
      fun `Abandon existing CAS2 application returns 200 with correct body`() {
        givenACas2LicenceCaseAdminUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, submittingUser)

            webTestClient.put()
              .uri("/cas2/applications/${application.id}/abandon")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk

            Assertions.assertNotNull(realApplicationRepository.findByIdAndServiceOrigin(application.id, Cas2ServiceOrigin.HDC)!!.abandonedAt)
          }
        }
      }
    }
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: Cas2UserEntity,
  ): Cas2ApplicationEntity {
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(userEntity)
      withData(
        data,
      )
    }

    return application
  }
}
