package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.external

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas3ExternalSuitableApplicationsTest : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetSuitableApplicationsByCrn {
    @Test
    fun `Get suitable application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/external/suitable-application/$crn")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get suitable application without correct JWT authority returns 401`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas3/external/suitable-application/$crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get suitable application returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withCrn(crn)
            withSubmittedAt(OffsetDateTime.now())
          }

          val suitableApplication = Cas3SuitableApplication(
            id = application.id,
            applicationStatus = ApplicationStatus.submitted,
            bookingStatus = null,
          )

          val response = webTestClient.get()
            .uri("/cas3/external/suitable-application/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Cas3SuitableApplication::class.java)
            .returnResult()
            .responseBody

          Assertions.assertThat(response).isEqualTo(suitableApplication)
        }
      }
    }

    @Test
    fun `Get suitable application returns inProgress application when only inProgress exists`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withCrn(crn)
            withSubmittedAt(null)
          }

          val suitableApplication = Cas3SuitableApplication(
            id = application.id,
            applicationStatus = ApplicationStatus.inProgress,
            bookingStatus = null,
          )

          val response = webTestClient.get()
            .uri("/cas3/external/suitable-application/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Cas3SuitableApplication::class.java)
            .returnResult()
            .responseBody

          Assertions.assertThat(response).isEqualTo(suitableApplication)
        }
      }
    }

    @Test
    fun `Get suitable application returns booking status when booking exists`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
            withProbationDeliveryUnit(probationDeliveryUnitFactory.produceAndPersist { withProbationRegion(user.probationRegion) })
          }

          val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withProbationRegion(user.probationRegion)
            withCrn(crn)
            withSubmittedAt(OffsetDateTime.now())
          }

          cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withApplication(application)
            withCrn(crn)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.now())
            withDepartureDate(LocalDate.now().plusDays(7))
            withStatus(Cas3BookingStatus.confirmed)
          }

          val suitableApplication = Cas3SuitableApplication(
            id = application.id,
            applicationStatus = ApplicationStatus.submitted,
            bookingStatus = Cas3BookingStatus.confirmed,
          )

          val response = webTestClient.get()
            .uri("/cas3/external/suitable-application/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Cas3SuitableApplication::class.java)
            .returnResult()
            .responseBody

          Assertions.assertThat(response).isEqualTo(suitableApplication)
        }
      }
    }

    @Test
    fun `Get suitable application returns not found`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas3/external/suitable-application/$crn")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
