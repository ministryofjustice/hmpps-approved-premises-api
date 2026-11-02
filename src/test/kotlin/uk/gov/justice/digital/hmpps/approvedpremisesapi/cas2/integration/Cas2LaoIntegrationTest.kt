package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesErrorResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesMultipleCases
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyCaseSummariesCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyUserAccessCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

/**
 * These tests validate the LAO behaviour of [Cas2OffenderService.getPersonByNomisIdOrCrn]
 * and [Cas2OffenderService.getPersonSummaries] (using the [cas2LaoStrategy])
 *
 * Unlike CAS1 and CAS3, CAS2 never checks the user's case access. Any offender with a current
 * restriction is treated as forbidden/limited access for every user, and a current exclusion
 * has no effect
 */
class Cas2LaoIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas2OffenderService: Cas2OffenderService

  /**
   * [Cas2OffenderService.getPersonByNomisIdOrCrn] resolves the user from the HTTP request,
   * which doesn't exist when calling the service directly
   */
  @MockkBean
  lateinit var cas2UserService: Cas2UserService

  @BeforeEach
  fun setupUser() {
    every { cas2UserService.getUserForRequest() } returns Cas2UserEntityFactory().produce()
  }

  enum class ExpectedResult { FULL, FORBIDDEN }

  enum class ExpectedSummaryResult { SUMMARY, LIMITED_ACCESS }

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

  @Nested
  inner class GetPersonSummaries {

    private fun givenACas2LaoStrategy(): LaoStrategy {
      val laoStrategy = givenACas2v2NomisUser().cas2LaoStrategy()
      assertThat(laoStrategy.restrictionHandling).isEqualTo(LaoStrategy.RestrictionHandling.BLOCK_REGARDLESS_OF_USER)
      assertThat(laoStrategy.exclusionHandling).isEqualTo(LaoStrategy.ExclusionHandling.IGNORE)
      return laoStrategy
    }

    @ParameterizedTest(name = "currentExclusion={0}, currentRestriction={1} -> {2}")
    @CsvSource(
      // currentExclusion, currentRestriction, expectedResult
      value = [
        "false,            false,              SUMMARY",
        "false,            true,               LIMITED_ACCESS",
        "true,             false,              SUMMARY",
        "true,             true,               LIMITED_ACCESS",
      ],
    )
    fun `returns the expected result for each combination of offender LAO status`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      expectedResult: ExpectedSummaryResult,
    ) {
      val laoStrategy = givenACas2LaoStrategy()
      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val results = cas2OffenderService.getPersonSummaries(laoStrategy, listOf(caseSummary.crn))

      assertThat(results).hasSize(1)
      when (expectedResult) {
        ExpectedSummaryResult.SUMMARY -> assertSummary(results.first(), caseSummary)
        ExpectedSummaryResult.LIMITED_ACCESS ->
          assertThat(results.first()).isEqualTo(Cas2PersonSummary.LimitedAccess(caseSummary.crn))
      }

      apDeliusContextVerifyCaseSummariesCalls(times = 1)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns NotFound when the CRN does not exist`() {
      val laoStrategy = givenACas2LaoStrategy()
      val crn = randomStringUpperCase(8)
      apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

      val results = cas2OffenderService.getPersonSummaries(laoStrategy, listOf(crn))

      assertThat(results).containsExactly(Cas2PersonSummary.NotFound(crn))
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns empty list and makes no upstream calls when no CRNs are provided`() {
      val laoStrategy = givenACas2LaoStrategy()

      val results = cas2OffenderService.getPersonSummaries(laoStrategy, emptyList())

      assertThat(results).isEmpty()
      apDeliusContextVerifyCaseSummariesCalls(times = 0)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns the expected result for each CRN when requesting multiple CRNs with mixed LAO status`() {
      val laoStrategy = givenACas2LaoStrategy()

      val notLao = CaseSummaryFactory().withNoLimitedAccess().produce()
      val excluded = CaseSummaryFactory().withCurrentExclusion(true).withCurrentRestriction(false).produce()
      val restricted = CaseSummaryFactory().withCurrentExclusion(false).withCurrentRestriction(true).produce()
      val excludedAndRestricted = CaseSummaryFactory()
        .withCurrentExclusion(true)
        .withCurrentRestriction(true)
        .produce()
      val notFoundCrn = randomStringUpperCase(8)

      val caseSummaries = listOf(notLao, excluded, restricted, excludedAndRestricted)
      val crns = caseSummaries.map { it.crn } + notFoundCrn

      apDeliusContextCaseSummariesMultipleCases(casesSummary = caseSummaries, crns = crns)

      val results = cas2OffenderService.getPersonSummaries(laoStrategy, crns)

      assertThat(results).hasSize(crns.size)
      assertSummary(results.forCrn(notLao.crn), notLao)
      assertSummary(results.forCrn(excluded.crn), excluded)
      assertThat(results.forCrn(restricted.crn)).isEqualTo(Cas2PersonSummary.LimitedAccess(restricted.crn))
      assertThat(results.forCrn(excludedAndRestricted.crn))
        .isEqualTo(Cas2PersonSummary.LimitedAccess(excludedAndRestricted.crn))
      assertThat(results.forCrn(notFoundCrn)).isEqualTo(Cas2PersonSummary.NotFound(notFoundCrn))

      apDeliusContextVerifyCaseSummariesCalls(times = 1)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    private fun List<Cas2PersonSummary>.forCrn(crn: String) = single { it.crn == crn }

    private fun assertSummary(result: Cas2PersonSummary, caseSummary: CaseSummary) {
      assertThat(result).isEqualTo(
        Cas2PersonSummary.Summary(
          crn = caseSummary.crn,
          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
        ),
      )
    }
  }
}
