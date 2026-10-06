package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external.Cas2ArrivalEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenASubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenAnUnsubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ArrivalDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASarClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class Cas2ExternalApplicationsTest : IntegrationTestBase() {
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
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external.Cas2ExternalApplicationsTest#isrCohorts")
    fun `Get suitable application returns ok`(cohort: Cas2Cohort) {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        val today = LocalDate.now()
        val createAt = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime().truncatedTo(ChronoUnit.MICROS)

        val application = givenASubmittedCas2Application(
          crn = crn,
          submittedAt = OffsetDateTime.parse("2023-01-01T00:00:00Z").truncatedTo(ChronoUnit.MICROS),
          cohort = cohort,
          createdAt = createAt,
          latestStatus = Cas2AssessmentStatus.MORE_INFO_REQUESTED,
        )

        val suitableApplication = Cas2SuitableApplication(
          uiUrl = "http://localhost:3000/assess/applications/${application.id}/overview",
          id = application.id,
          submittedApplication = Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.MORE_INFO_REQUESTED.apiName,
            submittedAt = application.submittedAt!!,
            offerDeclinedReason = null,
            cancelledReason = null,
          ),
          createdAt = application.createdAt,
          cohort = application.cohort?.apiType,
          createdBy = Cas2StaffDto(
            username = application.createdByUser.username,
            deliusStaffCode = application.createdByUser.deliusStaffCode,
            name = application.createdByUser.name,
            nomisStaffId = application.createdByUser.nomisStaffId,
            userType = Cas2UserTypeDto.valueOf(application.createdByUser.userType.name),
          ),
        )

        val response = webTestClient.get()
          .uri("/cas2/external/cases/${application.crn}/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsObject<Cas2SuitableApplication>()

        assertThat(response).isEqualTo(suitableApplication)
      }
    }

    @Test
    fun `Get suitable application returns latest application`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        val today = LocalDate.now()
        val latestTime = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime().truncatedTo(ChronoUnit.MICROS)
        val oldestTime = latestTime.minusDays(2)
        val submittedTime = OffsetDateTime.parse("2023-01-01T00:00:00Z").truncatedTo(ChronoUnit.MICROS)

        val latestApplication = givenASubmittedCas2Application(
          crn = crn,
          submittedAt = submittedTime,
          cohort = Cas2Cohort.ATCR,
          createdAt = latestTime,
          latestStatus = Cas2AssessmentStatus.MORE_INFO_REQUESTED,
        )

        givenASubmittedCas2Application(
          crn = crn,
          submittedAt = submittedTime,
          cohort = Cas2Cohort.ATCR,
          createdAt = oldestTime,
        )

        val suitableApplication = Cas2SuitableApplication(
          uiUrl = "http://localhost:3000/assess/applications/${latestApplication.id}/overview",
          id = latestApplication.id,
          submittedApplication = Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.MORE_INFO_REQUESTED.apiName,
            submittedAt = latestApplication.submittedAt!!,
            offerDeclinedReason = null,
            cancelledReason = null,
          ),
          createdAt = latestApplication.createdAt,
          cohort = latestApplication.cohort?.apiType,
          createdBy = Cas2StaffDto(
            username = latestApplication.createdByUser.username,
            deliusStaffCode = latestApplication.createdByUser.deliusStaffCode,
            name = latestApplication.createdByUser.name,
            nomisStaffId = latestApplication.createdByUser.nomisStaffId,
            userType = Cas2UserTypeDto.valueOf(latestApplication.createdByUser.userType.name),
          ),
        )

        val response = webTestClient.get()
          .uri("/cas2/external/cases/${latestApplication.crn}/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsObject<Cas2SuitableApplication>()
        assertThat(response).isEqualTo(suitableApplication)
      }
    }

    @Test
    fun `Get suitable application returns no content if all applications are abandoned`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->

        for (cohort in Cas2Cohort.entries) {
          givenAnUnsubmittedCas2Application(
            crn = crn,
            cohort = cohort,
            abandonedAt = OffsetDateTime.now(),
          )
        }

        webTestClient.get()
          .uri("/cas2/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNoContent
      }
    }

    @Test
    fun `Get suitable application returns no content if all applications are wrong cohort`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->

        val wrongCohorts = Cas2Cohort.entries - Cas2Cohort.isr().toSet()

        for (cohort in wrongCohorts) {
          givenASubmittedCas2Application(
            crn = crn,
            cohort = cohort,
          )
        }

        webTestClient.get()
          .uri("/cas2/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNoContent
      }
    }

    @Test
    fun `Get suitable application returns not found if no cas2 applications exist`() {
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

  @Nested
  inner class Cas2ExternalArrivalsTest {

    @Test
    fun `Returns 401 unauthorised if jwt is invalid`() {
      webTestClient.post()
        .uri("/cas2/external/cases/CRN123/applications/1fd64d01-79ed-44d9-9cfe-98176fae517e/arrival")
        .header("Authorization", "Bearer invalid")
        .bodyValue(
          Cas2ArrivalDto(
            markedAsArrivedDateTime = Instant.now(),
            arrivedByUsername = "username",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Returns 403 forbidden if jwt is valid but the client does not have the right role`() {
      givenASarClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.post()
          .uri("/cas2/external/cases/CRN123/applications/1fd64d01-79ed-44d9-9cfe-98176fae517e/arrival")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .bodyValue(
            Cas2ArrivalDto(
              markedAsArrivedDateTime = Instant.now(),
              arrivedByUsername = "username",
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Returns 404 not found if the CRN does not exist`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.post()
          .uri("/cas2/external/cases/CRN123/applications/1fd64d01-79ed-44d9-9cfe-98176fae517e/arrival")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .bodyValue(
            Cas2ArrivalDto(
              markedAsArrivedDateTime = Instant.now(),
              arrivedByUsername = "username",
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Returns 404 not found if the application is not found`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->

        val application = givenASubmittedCas2Application(
          crn = "CRN123",
        )

        webTestClient.post()
          .uri("/cas2/external/cases/CRN456/applications/${application.id}/arrival")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .bodyValue(
            Cas2ArrivalDto(
              markedAsArrivedDateTime = Instant.now(),
              arrivedByUsername = "username",
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Returns 400 bad request if the arrival datetime is in the future`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->

        val crn = "CRN123"
        val arrivalDateTime = Instant.now().plusSeconds(3600)
        val arrivedByUsername = "username"

        val application = givenASubmittedCas2Application(crn = crn)

        cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        webTestClient.post()
          .uri("/cas2/external/cases/$crn/applications/${application.id}/arrival")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .bodyValue(
            Cas2ArrivalDto(
              markedAsArrivedDateTime = arrivalDateTime,
              arrivedByUsername = arrivedByUsername,
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }

    @Test
    fun `Returns OK and creates a domain event when recording arrival`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->

        val crn = "CRN123"
        val arrivalDateTime = Instant.parse("2025-12-04T12:53:10Z")
        val arrivedByUsername = "username"

        val application = givenASubmittedCas2Application(crn = crn)

        cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        webTestClient.post()
          .uri("/cas2/external/cases/$crn/applications/${application.id}/arrival")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .bodyValue(
            Cas2ArrivalDto(
              markedAsArrivedDateTime = arrivalDateTime,
              arrivedByUsername = arrivedByUsername,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated

        val persistedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
          application.id,
          DomainEventType.CAS2_PERSON_ARRIVED,
        )

        assertThat(persistedEvent.crn).isEqualTo(crn)
        assertThat(persistedEvent.applicationId).isEqualTo(application.id)
        assertThat(persistedEvent.type).isEqualTo(DomainEventType.CAS2_PERSON_ARRIVED)

        val arrivalEvent = jsonMapper.readValue<Cas2ArrivalEvent>(persistedEvent.data)
        assertThat(arrivalEvent.id).isNotNull()
        assertThat(arrivalEvent.timestamp).isNotNull()
        assertThat(arrivalEvent.eventType).isEqualTo(EventType.arrived)

        val arrivalEventDetails = arrivalEvent.eventDetails
        assertThat(arrivalEventDetails.markedAsArrivedDateTime).isEqualTo(arrivalDateTime)
        assertThat(arrivalEventDetails.arrivedByUsername).isEqualTo(arrivedByUsername)
      }
    }
  }

  companion object {

    @JvmStatic
    fun isrCohorts(): List<Arguments> = Cas2Cohort.isr().map { Arguments.of(it) }
  }
}
