package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HealthAndMedicationApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DietAndAllergyResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HealthAndMedicationService

class HealthAndMedicationServiceTest {
  private val mockMedicationApiClient = mockk<HealthAndMedicationApiClient>()
  private val mockCaseService = mockk<CaseService>()

  private val healthAndMedicationService = HealthAndMedicationService(
    mockMedicationApiClient,
    mockCaseService,
  )

  @Nested
  inner class GetDietAndAllergyDetails {
    @Test
    fun `returns NotFound when no offender found for CRN`() {
      val crn = "CRN123"

      every { mockCaseService.getCase(crn) } returns null

      val result = healthAndMedicationService.getDietAndAllergyDetails(crn)

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `returns NotFound when API returns 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS123"

      every { mockCaseService.getCase(crn) } returns CaseDtoFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockMedicationApiClient.getDietAndAllergyDetails(nomsNumber) } returns ClientResult.Failure.StatusCode(
        method = HttpMethod.GET,
        path = "/prisoners/$nomsNumber",
        status = HttpStatus.NOT_FOUND,
        body = null,
      )

      val result = healthAndMedicationService.getDietAndAllergyDetails(crn)

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `returns Unauthorised when API returns 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS123"

      every { mockCaseService.getCase(crn) } returns CaseDtoFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockMedicationApiClient.getDietAndAllergyDetails(nomsNumber) } returns ClientResult.Failure.StatusCode(
        method = HttpMethod.GET,
        path = "/prisoners/$nomsNumber",
        status = HttpStatus.FORBIDDEN,
        body = null,
      )

      val result = healthAndMedicationService.getDietAndAllergyDetails(crn)

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `throws exception when API returns other error status`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS123"

      every { mockCaseService.getCase(crn) } returns CaseDtoFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockMedicationApiClient.getDietAndAllergyDetails(nomsNumber) } returns ClientResult.Failure.StatusCode(
        method = HttpMethod.GET,
        path = "/prisoners/$nomsNumber",
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        body = null,
      )

      assertThatExceptionOfType(RuntimeException::class.java).isThrownBy {
        healthAndMedicationService.getDietAndAllergyDetails(crn)
      }
    }

    @Test
    fun `returns Success when API returns 200`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS123"
      val response = DietAndAllergyResponseFactory().produce()

      every { mockCaseService.getCase(crn) } returns CaseDtoFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockMedicationApiClient.getDietAndAllergyDetails(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = response,
      )

      val result = healthAndMedicationService.getDietAndAllergyDetails(crn)

      assertThat(result is CasResult.Success).isTrue
      assertThat((result as CasResult.Success<DietAndAllergyResponse>).value).isEqualTo(response)
    }
  }
}
