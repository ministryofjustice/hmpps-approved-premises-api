package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.LicenceApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LicenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LicenceSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LicenceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult

class LicenceServiceTest {

  private val mockLicenceApiClient = mockk<LicenceApiClient>()
  private val service = LicenceService(mockLicenceApiClient)

  private fun licenceSummary(id: Long, status: LicenceStatus): LicenceSummary = LicenceSummaryFactory()
    .withId(id)
    .withStatus(status)
    .withCrn("X12345")
    .withLicenceType(LicenceType.AP)
    .produce()

  private fun licence(id: Long): Licence = LicenceFactory()
    .withId(id)
    .withStatus(LicenceStatus.ACTIVE)
    .withCrn("X12345")
    .withLicenceType(LicenceType.AP)
    .produce()

  @Nested
  inner class GetLicence {
    @Test
    fun `returns NotFound when summaries endpoint returns 404`() {
      val crn = "X12345"
      every { mockLicenceApiClient.getLicenceSummaries(crn) } returns StatusCode(HttpMethod.GET, "/public/licence-summaries/crn/$crn", HttpStatus.NOT_FOUND, null)

      val result = service.getLicence(crn)

      assertThatCasResult(result).isNotFound("Licence", crn)
    }

    @Test
    fun `returns Unauthorised when summaries endpoint returns 403`() {
      val crn = "X12345"
      every { mockLicenceApiClient.getLicenceSummaries(crn) } returns StatusCode(HttpMethod.GET, "/public/licence-summaries/crn/$crn", HttpStatus.FORBIDDEN, null)

      val result = service.getLicence(crn)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `returns NotFound when no ACTIVE licence summary exists`() {
      val crn = "X12345"
      every { mockLicenceApiClient.getLicenceSummaries(crn) } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(
          licenceSummary(1, LicenceStatus.APPROVED),
          licenceSummary(2, LicenceStatus.VARIATION_IN_PROGRESS),
        ),
      )

      val result = service.getLicence(crn)

      assertThatCasResult(result).isNotFound("Licence", crn)
    }

    @Test
    fun `returns Success with licence details when ACTIVE summary exists`() {
      val crn = "X12345"
      val activeId = 99L
      every { mockLicenceApiClient.getLicenceSummaries(crn) } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(
          licenceSummary(1, LicenceStatus.APPROVED),
          licenceSummary(activeId, LicenceStatus.ACTIVE),
        ),
      )
      val licence = licence(activeId)
      every { mockLicenceApiClient.getLicenceDetails(activeId) } returns ClientResult.Success(HttpStatus.OK, licence)

      val result = service.getLicence(crn)

      assertThatCasResult(result).isSuccess().with { assertThat(it).isEqualTo(licence) }
    }

    @Test
    fun `returns NotFound when details endpoint returns 404 for ACTIVE summary`() {
      val crn = "X12345"
      val activeId = 100L
      every { mockLicenceApiClient.getLicenceSummaries(crn) } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(
          licenceSummary(activeId, LicenceStatus.ACTIVE),
        ),
      )
      every { mockLicenceApiClient.getLicenceDetails(activeId) } returns StatusCode(HttpMethod.GET, "/public/licences/id/$activeId", HttpStatus.NOT_FOUND, null)

      val result = service.getLicence(crn)

      assertThatCasResult(result).isNotFound("Licence", crn)
    }

    @Test
    fun `returns Unauthorised when details endpoint returns 403 for ACTIVE summary`() {
      val crn = "X12345"
      val activeId = 101L
      every { mockLicenceApiClient.getLicenceSummaries(crn) } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(
          licenceSummary(activeId, LicenceStatus.ACTIVE),
        ),
      )
      every { mockLicenceApiClient.getLicenceDetails(activeId) } returns StatusCode(HttpMethod.GET, "/public/licences/id/$activeId", HttpStatus.FORBIDDEN, null)

      val result = service.getLicence(crn)

      assertThatCasResult(result).isUnauthorised()
    }
  }
}
