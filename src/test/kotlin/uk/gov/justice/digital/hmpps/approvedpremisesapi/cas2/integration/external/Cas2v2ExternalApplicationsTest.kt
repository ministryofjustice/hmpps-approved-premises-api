package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import java.time.OffsetDateTime

class Cas2v2ExternalApplicationsTest : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetSuitableApplicationsByCrn {
    @Test
    fun `Get suitable application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/external/cases/$crn/applications/suitable")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get suitable application without correct JWT authority returns 403`() {
      givenACas2v2PomUser { _, jwt ->
        webTestClient.get()
          .uri("/cas2/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(Cas2Cohort::class, names = ["ATCR", "HCRD", "HEFR", "ISC", "RARR", "FROM_AP"])
    fun `Get suitable application returns ok`(cohort: Cas2Cohort) {
      givenACas2Assessor { assessor, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(
              cas2UserEntityFactory.produceAndPersist(),
            )
            withCrn(crn)
            withSubmittedAt(OffsetDateTime.now())
            withCohort(cohort)
            withData("{}")
            withStatusUpdates(mutableListOf())
            withAbandonedAt(null)
          }

          val statusUpdate = cas2StatusUpdateEntityFactory.produceAndPersist {
            withAssessor(assessor)
            withApplication(application)
            withLabel("finished")
          }

          application.statusUpdates?.add(statusUpdate)

          cas2ApplicationRepository.save(application)

          val suitableApplication = Cas2SuitableApplication(
            uiUrl = "http://localhost:3000/assess/applications/${application.id}/overview",
            application = Cas2ExternalApplicationDto(
              status = "finished",
              id = application.id,
            ),
          )

          val response = webTestClient.get()
            .uri("/cas2/external/cases/${application.crn}/applications/suitable")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Cas2SuitableApplication::class.java)
            .returnResult()
            .responseBody

          assertThat(response).isEqualTo(suitableApplication)
        }
      }
    }

    @Test
    fun `Get suitable application returns latest application`() {
      givenACas2Assessor { assessor, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val latestTime = OffsetDateTime.now()
          val oldestTime = latestTime.minusDays(2)
          val latestApplication = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(
              cas2UserEntityFactory.produceAndPersist(),
            )
            withCrn(crn)
            withSubmittedAt(OffsetDateTime.now())
            withCohort(Cas2Cohort.ATCR)
            withData("{}")
            withStatusUpdates(mutableListOf())
            withCreatedAt(latestTime)
            withAbandonedAt(null)
          }

          cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(
              cas2UserEntityFactory.produceAndPersist(),
            )
            withCrn(crn)
            withSubmittedAt(OffsetDateTime.now())
            withCohort(Cas2Cohort.ATCR)
            withData("{}")
            withStatusUpdates(mutableListOf())
            withCreatedAt(oldestTime)
            withAbandonedAt(null)
          }

          val statusUpdate = cas2StatusUpdateEntityFactory.produceAndPersist {
            withAssessor(assessor)
            withApplication(latestApplication)
            withLabel("finished")
          }

          latestApplication.statusUpdates?.add(statusUpdate)

          cas2ApplicationRepository.save(latestApplication)

          val suitableApplication = Cas2SuitableApplication(
            uiUrl = "http://localhost:3000/assess/applications/${latestApplication.id}/overview",
            application = Cas2ExternalApplicationDto(
              status = "finished",
              id = latestApplication.id,
            ),
          )

          val response = webTestClient.get()
            .uri("/cas2/external/cases/${latestApplication.crn}/applications/suitable")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Cas2SuitableApplication::class.java)
            .returnResult()
            .responseBody

          assertThat(response).isEqualTo(suitableApplication)
        }
      }
    }

    @Test
    fun `Get suitable application returns no content if all applications are abandoned`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.ATCR)
          withData("{}")
          withAbandonedAt(OffsetDateTime.now())
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.RARR)
          withData("{}")
          withAbandonedAt(OffsetDateTime.now())
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.HEFR)
          withData("{}")
          withAbandonedAt(OffsetDateTime.now())
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.HCRD)
          withData("{}")
          withAbandonedAt(OffsetDateTime.now())
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.FROM_AP)
          withData("{}")
          withAbandonedAt(OffsetDateTime.now())
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.ISC)
          withData("{}")
          withAbandonedAt(OffsetDateTime.now())
        }

        webTestClient.get()
          .uri("/cas2/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNoContent
          .expectBody(Cas2SuitableApplication::class.java)
          .returnResult()
          .responseBody
      }
    }

    @Test
    fun `Get suitable application returns no content if all applications are wrong cohort`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.COURT_BAIL)
          withData("{}")
          withAbandonedAt(null)
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.PRISON_BAIL)
          withData("{}")
          withAbandonedAt(null)
        }
        cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(
            cas2UserEntityFactory.produceAndPersist(),
          )
          withCrn(crn)
          withCohort(Cas2Cohort.HDC)
          withData("{}")
          withAbandonedAt(null)
        }

        webTestClient.get()
          .uri("/cas2/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNoContent
          .expectBody(Cas2SuitableApplication::class.java)
          .returnResult()
          .responseBody
      }
    }
  }

  @Test
  fun `Get suitable application returns not found`() {
    givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
      webTestClient.get()
        .uri("/cas2/external/cases/$crn/applications/suitable")
        .header("Authorization", "Bearer $clientCredentialsJwt")
        .exchange()
        .expectStatus()
        .isNoContent
    }
  }
}
