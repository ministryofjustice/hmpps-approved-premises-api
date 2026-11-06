package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesErrorResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesMultipleCases
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextUserAccessSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyCaseSummariesCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyUserAccessCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

/**
 * These tests validate the LAO behaviour of [Cas2OffenderService.getPersonByNomisIdOrCrnForUser]
 * and [Cas2OffenderService.getPersonSummaries] (using the [cas2LaoStrategy])
 *
 * The [cas2LaoStrategy] depends on the type of the calling user:
 *
 * - NOMIS users never have their case access checked. Any offender with a current restriction is
 *   treated as forbidden/limited access, and a current exclusion has no effect
 * - Delius users have their case access checked using their username, so both restrictions and
 *   exclusions are applied
 * - External users are never restricted, so LAO status has no effect
 */
class Cas2LaoIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas2OffenderService: Cas2OffenderService

  enum class ExpectedResult { FULL, FORBIDDEN }

  enum class ExpectedSummaryResult { SUMMARY, LIMITED_ACCESS }

  private fun givenADeliusUser() = Cas2UserEntityFactory().withUserType(Cas2UserType.DELIUS).produce()

  private fun givenANomisUser() = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()

  private fun givenAnExternalUser() = Cas2UserEntityFactory().withUserType(Cas2UserType.EXTERNAL).produce()

  private fun givenAnOffenderWithAccess(
    user: Cas2UserEntity,
    currentExclusion: Boolean,
    currentRestriction: Boolean,
    userExcluded: Boolean,
    userRestricted: Boolean,
  ): CaseSummary {
    val caseSummary = CaseSummaryFactory()
      .withCurrentExclusion(currentExclusion)
      .withCurrentRestriction(currentRestriction)
      .produce()

    apDeliusContextCaseSummariesSingleCase(caseSummary)
    apDeliusContextUserAccessSingleCase(
      CaseAccessFactory()
        .withCrn(caseSummary.crn)
        .withUserExcluded(userExcluded)
        .withUserRestricted(userRestricted)
        .produce(),
      username = user.username,
    )
    return caseSummary
  }

  private fun assertSummary(result: Cas2PersonSummary, caseSummary: CaseSummary) {
    assertThat(result).isEqualTo(
      Cas2PersonSummary.Summary(
        crn = caseSummary.crn,
        name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
      ),
    )
  }

  @Nested
  inner class DeliusUser {

    @ParameterizedTest(
      name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> {4} (access checked={5})",
    )
    @CsvSource(
      // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedResult, userAccessChecked
      value = [
        "false,            false,              false,        false,          FULL,           false",
        "false,            true,               false,        false,          FULL,           true",
        "false,            true,               false,        true,           FORBIDDEN,      true",
        "true,             false,              false,        false,          FULL,           true",
        "true,             false,              true,         false,          FORBIDDEN,      true",
        "true,             true,               false,        false,          FULL,           true",
        "true,             true,               false,        true,           FORBIDDEN,      true",
        "true,             true,               true,         false,          FORBIDDEN,      true",
        "true,             true,               true,         true,           FORBIDDEN,      true",
      ],
    )
    @SuppressWarnings("LongParameterList")
    fun `getPersonByNomisIdOrCrn returns the expected result for each combination of offender LAO status and user access`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      userExcluded: Boolean,
      userRestricted: Boolean,
      expectedResult: ExpectedResult,
      userAccessChecked: Boolean,
    ) {
      val requestUser = givenADeliusUser()
      val caseSummary =
        givenAnOffenderWithAccess(requestUser, currentExclusion, currentRestriction, userExcluded, userRestricted)

      val result = cas2OffenderService.getPersonByNomisIdOrCrnForUser(caseSummary.crn) { requestUser }

      when (expectedResult) {
        ExpectedResult.FULL -> {
          assertThat(result).isInstanceOf(Cas2v2OffenderSearchResult.Success.Full::class.java)
          result as Cas2v2OffenderSearchResult.Success.Full
          assertThat(result.person.crn).isEqualTo(caseSummary.crn)
        }

        ExpectedResult.FORBIDDEN ->
          assertThat(result).isEqualTo(Cas2v2OffenderSearchResult.Forbidden(caseSummary.crn))
      }

      apDeliusContextVerifyUserAccessCalls(times = if (userAccessChecked) 1 else 0, username = requestUser.username)
    }

    @ParameterizedTest(
      name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> {4} (access checked={5})",
    )
    @CsvSource(
      // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedResult, userAccessChecked
      value = [
        "false,            false,              false,        false,          SUMMARY,        false",
        "false,            true,               false,        false,          SUMMARY,        true",
        "false,            true,               false,        true,           LIMITED_ACCESS, true",
        "true,             false,              false,        false,          SUMMARY,        true",
        "true,             false,              true,         false,          LIMITED_ACCESS, true",
        "true,             true,               false,        false,          SUMMARY,        true",
        "true,             true,               false,        true,           LIMITED_ACCESS, true",
        "true,             true,               true,         false,          LIMITED_ACCESS, true",
        "true,             true,               true,         true,           LIMITED_ACCESS, true",
      ],
    )
    @SuppressWarnings("LongParameterList")
    fun `getPersonSummaries returns the expected result for each combination of offender LAO status and user access`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      userExcluded: Boolean,
      userRestricted: Boolean,
      expectedResult: ExpectedSummaryResult,
      userAccessChecked: Boolean,
    ) {
      val user = givenADeliusUser()
      val laoStrategy = user.cas2LaoStrategy()
      assertThat(laoStrategy).isEqualTo(LaoStrategy.checkUserAccess(user.username))
      val caseSummary =
        givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)

      val results = cas2OffenderService.getPersonSummaries(laoStrategy, listOf(caseSummary.crn))

      assertThat(results).hasSize(1)
      when (expectedResult) {
        ExpectedSummaryResult.SUMMARY -> assertSummary(results.first(), caseSummary)
        ExpectedSummaryResult.LIMITED_ACCESS ->
          assertThat(results.first()).isEqualTo(Cas2PersonSummary.LimitedAccess(caseSummary.crn))
      }

      apDeliusContextVerifyUserAccessCalls(times = if (userAccessChecked) 1 else 0, username = user.username)
    }
  }

  @Nested
  inner class ExternalUser {

    @ParameterizedTest(name = "currentExclusion={0}, currentRestriction={1}")
    @CsvSource(
      value = [
        "false, false",
        "false, true",
        "true,  false",
        "true,  true",
      ],
    )
    fun `getPersonByNomisIdOrCrn always returns the full person and never checks user access`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val requestUser = givenAnExternalUser()
      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val result = cas2OffenderService.getPersonByNomisIdOrCrnForUser(caseSummary.crn) { requestUser }

      assertThat(result).isInstanceOf(Cas2v2OffenderSearchResult.Success.Full::class.java)
      result as Cas2v2OffenderSearchResult.Success.Full
      assertThat(result.person.crn).isEqualTo(caseSummary.crn)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @ParameterizedTest(name = "currentExclusion={0}, currentRestriction={1}")
    @CsvSource(
      value = [
        "false, false",
        "false, true",
        "true,  false",
        "true,  true",
      ],
    )
    fun `getPersonSummaries always returns the summary and never checks user access`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val laoStrategy = givenAnExternalUser().cas2LaoStrategy()
      assertThat(laoStrategy).isEqualTo(LaoStrategy.NeverRestricted)
      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val results = cas2OffenderService.getPersonSummaries(laoStrategy, listOf(caseSummary.crn))

      assertThat(results).hasSize(1)
      assertSummary(results.first(), caseSummary)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }
  }

  @Nested
  inner class NomisUser {

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

      val result = cas2OffenderService.getPersonByNomisIdOrCrnForUser(caseSummary.crn) { givenANomisUser() }

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
  }

  @Nested
  inner class Common {
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
  }

  private fun List<Cas2PersonSummary>.forCrn(crn: String) = single { it.crn == crn }

  private fun givenACas2LaoStrategy(): LaoStrategy {
    val laoStrategy = givenACas2v2NomisUser().cas2LaoStrategy()
    assertThat(laoStrategy.restrictionHandling).isEqualTo(LaoStrategy.RestrictionHandling.BLOCK_REGARDLESS_OF_USER)
    assertThat(laoStrategy.exclusionHandling).isEqualTo(LaoStrategy.ExclusionHandling.IGNORE)
    return laoStrategy
  }
}
