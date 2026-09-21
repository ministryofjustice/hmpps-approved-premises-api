package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenASubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenAnUnsubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
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
          latestStatusName = "moreInfoRequested",
          createdAt = createAt,
        )

        val suitableApplication = Cas2SuitableApplication(
          uiUrl = "http://localhost:3000/assess/applications/${application.id}/overview",
          id = application.id,
          submittedApplication = Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.MORE_INFO_REQUESTED,
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
          latestStatusName = "moreInfoRequested",
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
            latestAssessmentStatus = Cas2AssessmentStatus.MORE_INFO_REQUESTED,
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
    fun `Get suitable application returns null when applications have a conditional release date in the past`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        val latestTime = OffsetDateTime.now()
        val submittedTime = OffsetDateTime.parse("2023-01-01T00:00:00Z").truncatedTo(ChronoUnit.MICROS)

        val latestApplication = givenASubmittedCas2Application(
          crn = crn,
          submittedAt = submittedTime,
          cohort = Cas2Cohort.ATCR,
          createdAt = latestTime,
          latestStatusName = "moreInfoRequested",
          conditionalReleaseDate = LocalDate.now().minusDays(1),
        )

        webTestClient.get()
          .uri("/cas2/external/cases/${latestApplication.crn}/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNoContent
      }
    }

    @Test
    fun `Get suitable application returns null when applications have a null conditional release date and were created more than 2 months in the past`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        val latestTime = OffsetDateTime.now().minusMonths(2)
        val submittedTime = OffsetDateTime.parse("2023-01-01T00:00:00Z").truncatedTo(ChronoUnit.MICROS)

        val latestApplication = givenASubmittedCas2Application(
          crn = crn,
          submittedAt = submittedTime,
          cohort = Cas2Cohort.ATCR,
          createdAt = latestTime,
          latestStatusName = "moreInfoRequested",
          conditionalReleaseDate = null,
        )

        webTestClient.get()
          .uri("/cas2/external/cases/${latestApplication.crn}/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNoContent
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

  companion object {

    @JvmStatic
    fun isrCohorts(): List<Arguments> = Cas2Cohort.isr().map { Arguments.of(it) }
  }
}
