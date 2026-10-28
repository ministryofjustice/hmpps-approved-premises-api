package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesErrorResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyCaseSummariesCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyUserAccessCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

/**
 * These tests validate the LAO behaviour of [Cas2OffenderService.getPersonByNomisIdOrCrn]
 *
 * Unlike CAS1 and CAS3, CAS2 doesn't use a LaoStrategy and never checks the user's case access.
 * Any offender with a current restriction is treated as forbidden for every user, and a
 * current exclusion has no effect
 */
class Cas2LaoIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas2OffenderService: Cas2OffenderService

  enum class ExpectedResult { FULL, FORBIDDEN }

  @Nested
  inner class GetPersonByNomisIdOrCrn {

    @ParameterizedTest(name = "currentExclusion={0}, currentRestriction={1} -> {2}")
    @CsvSource(
      // currentExclusion, currentRestriction, expectedResult
      value = [
        "false,            false,              FULL",
        "false,            true,               FORBIDDEN",
        "true,             false,              FULL",
        "true,             true,               FORBIDDEN",
      ],
    )
    fun `returns the expected result for each combination of offender LAO status`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      expectedResult: ExpectedResult,
    ) {
      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(caseSummary.crn)

      when (expectedResult) {
        ExpectedResult.FULL -> {
          assertThat(result).isInstanceOf(Cas2v2OffenderSearchResult.Success.Full::class.java)
          result as Cas2v2OffenderSearchResult.Success.Full
          assertThat(result.nomisIdOrCrn).isEqualTo(caseSummary.crn)
          assertThat(result.person.crn).isEqualTo(caseSummary.crn)
        }

        ExpectedResult.FORBIDDEN ->
          assertThat(result).isEqualTo(Cas2v2OffenderSearchResult.Forbidden(caseSummary.crn))
      }

      apDeliusContextVerifyCaseSummariesCalls(times = 1)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns NotFound when the offender does not exist`() {
      val crn = randomStringUpperCase(8)
      apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(crn)

      assertThat(result).isEqualTo(Cas2v2OffenderSearchResult.NotFound(crn))
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns NotFound when the case summaries call returns a 404`() {
      val crn = randomStringUpperCase(8)
      apDeliusContextCaseSummariesErrorResponse(404)

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(crn)

      assertThat(result).isEqualTo(Cas2v2OffenderSearchResult.NotFound(crn))
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns Forbidden when the case summaries call returns a 403`() {
      val crn = randomStringUpperCase(8)
      apDeliusContextCaseSummariesErrorResponse(403)

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(crn)

      assertThat(result).isInstanceOf(Cas2v2OffenderSearchResult.Forbidden::class.java)
      assertThat(result.nomisIdOrCrn).isEqualTo(crn)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns Unknown when the case summaries call fails unexpectedly`() {
      val crn = randomStringUpperCase(8)
      apDeliusContextCaseSummariesErrorResponse(500)

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(crn)

      assertThat(result).isInstanceOf(Cas2v2OffenderSearchResult.Unknown::class.java)
      assertThat(result.nomisIdOrCrn).isEqualTo(crn)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }
  }
}
