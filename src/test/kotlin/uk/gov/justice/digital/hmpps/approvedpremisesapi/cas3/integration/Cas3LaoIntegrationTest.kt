package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesMultipleCases
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextUserAccessAddCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextUserAccessSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyCaseSummariesCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyUserAccessCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

/**
 * These tests validate the LaoStrategies used by CAS3 by calling OffenderServices directly
 *
 * This has been added because testing all LAO permutations for all CAS3 endpoints that may
 * be impacted by LAO is impractical
 */
class Cas3LaoIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var offenderService: OffenderService

  enum class ExpectedResult { FULL, RESTRICTED }

  @Nested
  inner class GetPersonSummaryInfoResults {

    @ParameterizedTest(
      name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> {4} (access checked={5})",
    )
    @CsvSource(
      // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedResult, userAccessChecked
      value = [
        "false,            false,              false,        false,          FULL,           false",
        "false,            true,               false,        false,          FULL,           true",
        "false,            true,               false,        true,           RESTRICTED,     true",
        "true,             false,              false,        false,          FULL,           true",
        "true,             false,              true,         false,          RESTRICTED,     true",
        "true,             true,               false,        false,          FULL,           true",
        "true,             true,               false,        true,           RESTRICTED,     true",
        "true,             true,               true,         false,          RESTRICTED,     true",
        "true,             true,               true,         true,           RESTRICTED,     true",
      ],
    )
    @SuppressWarnings("LongParameterList")
    fun `returns the expected result for each combination of offender LAO status and user access`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      userExcluded: Boolean,
      userRestricted: Boolean,
      expectedResult: ExpectedResult,
      userAccessChecked: Boolean,
    ) {
      val (user, _) = givenAUser()

      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      val crn = caseSummary.crn

      apDeliusContextCaseSummariesSingleCase(caseSummary)
      apDeliusContextUserAccessSingleCase(
        CaseAccessFactory()
          .withCrn(crn)
          .withUserExcluded(userExcluded)
          .withUserRestricted(userRestricted)
          .produce(),
        username = user.deliusUsername,
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(crn),
        laoStrategy = user.cas3LaoStrategy(),
        includeTier = false,
      )

      assertThat(results).hasSize(1)

      when (expectedResult) {
        ExpectedResult.FULL -> assertFull(results.first(), caseSummary)
        ExpectedResult.RESTRICTED -> assertRestricted(results.first(), caseSummary)
      }

      val expectedUserAccessCalls = if (userAccessChecked) 1 else 0
      apDeliusContextVerifyUserAccessCalls(times = expectedUserAccessCalls, username = user.deliusUsername)
    }

    @Test
    fun `returns NotFound when the CRN does not exist and does not check user access`() {
      val (user, _) = givenAUser()
      val crn = randomStringUpperCase(8)

      apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(crn),
        laoStrategy = user.cas3LaoStrategy(),
        includeTier = false,
      )

      assertThat(results).hasSize(1)
      assertNotFound(results.first(), crn)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns NotFound when the offender is LAO but no user access information is returned`() {
      val (user, _) = givenAUser()

      val caseSummary = CaseSummaryFactory()
        .withCurrentRestriction(true)
        .produce()

      apDeliusContextCaseSummariesSingleCase(caseSummary)
      apDeliusContextUserAccessAddCase(emptyList(), username = user.deliusUsername)

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(caseSummary.crn),
        laoStrategy = user.cas3LaoStrategy(),
        includeTier = false,
      )

      assertThat(results).hasSize(1)
      assertNotFound(results.first(), caseSummary.crn)
      apDeliusContextVerifyUserAccessCalls(times = 1, username = user.deliusUsername)
    }

    @Test
    fun `returns empty list and makes no upstream calls when no CRNs are provided`() {
      val (user, _) = givenAUser()

      val results = offenderService.getPersonSummaryInfoResults(
        crns = emptySet(),
        laoStrategy = user.cas3LaoStrategy(),
      )

      assertThat(results).isEmpty()
      apDeliusContextVerifyCaseSummariesCalls(times = 0)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns the expected result for each CRN when requesting multiple CRNs with mixed LAO status and user access`() {
      val (user, _) = givenAUser()

      val notLao = CaseSummaryFactory().withNoLimitedAccess().produce()
      val excludedUserExcluded = CaseSummaryFactory().withCurrentExclusion(true).produce()
      val excludedUserHasAccess = CaseSummaryFactory().withCurrentExclusion(true).produce()
      val restrictedUserRestricted = CaseSummaryFactory().withCurrentRestriction(true).produce()
      val restrictedUserHasAccess = CaseSummaryFactory().withCurrentRestriction(true).produce()
      val excludedAndRestrictedUserExcludedAndRestricted = CaseSummaryFactory()
        .withCurrentExclusion(true)
        .withCurrentRestriction(true)
        .produce()
      val laoWithNoAccessInfo = CaseSummaryFactory().withCurrentRestriction(true).produce()
      val notFoundCrn = randomStringUpperCase(8)

      val caseSummaries = listOf(
        notLao,
        excludedUserExcluded,
        excludedUserHasAccess,
        restrictedUserRestricted,
        restrictedUserHasAccess,
        excludedAndRestrictedUserExcludedAndRestricted,
        laoWithNoAccessInfo,
      )
      val crns = caseSummaries.map { it.crn } + notFoundCrn

      apDeliusContextCaseSummariesMultipleCases(casesSummary = caseSummaries, crns = crns)
      apDeliusContextUserAccessAddCase(
        listOf(
          CaseAccessFactory().withCrn(excludedUserExcluded.crn).withUserExcluded(true).produce(),
          CaseAccessFactory().withCrn(excludedUserHasAccess.crn).withAccess().produce(),
          CaseAccessFactory().withCrn(restrictedUserRestricted.crn).withUserRestricted(true).produce(),
          CaseAccessFactory().withCrn(restrictedUserHasAccess.crn).withAccess().produce(),
          CaseAccessFactory()
            .withCrn(excludedAndRestrictedUserExcludedAndRestricted.crn)
            .withUserExcluded(true)
            .withUserRestricted(true)
            .produce(),
        ),
        username = user.deliusUsername,
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = crns.toSet(),
        laoStrategy = user.cas3LaoStrategy(),
        includeTier = false,
      )

      assertThat(results).hasSize(crns.size)
      assertFull(results.forCrn(notLao.crn), notLao)
      assertRestricted(results.forCrn(excludedUserExcluded.crn), excludedUserExcluded)
      assertFull(results.forCrn(excludedUserHasAccess.crn), excludedUserHasAccess)
      assertRestricted(results.forCrn(restrictedUserRestricted.crn), restrictedUserRestricted)
      assertFull(results.forCrn(restrictedUserHasAccess.crn), restrictedUserHasAccess)
      assertRestricted(
        results.forCrn(excludedAndRestrictedUserExcludedAndRestricted.crn),
        excludedAndRestrictedUserExcludedAndRestricted,
      )
      assertNotFound(results.forCrn(laoWithNoAccessInfo.crn), laoWithNoAccessInfo.crn)
      assertNotFound(results.forCrn(notFoundCrn), notFoundCrn)

      apDeliusContextVerifyUserAccessCalls(times = 1, username = user.deliusUsername)
    }
  }

  @Nested
  inner class CanAccessOffender {

    @ParameterizedTest(
      name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> canAccess={4}",
    )
    @CsvSource(
      // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedCanAccess
      value = [
        "false,            false,              false,        false,          true",
        "false,            true,               false,        false,          true",
        "false,            true,               false,        true,           false",
        "true,             false,              false,        false,          true",
        "true,             false,              true,         false,          false",
        "true,             true,               false,        false,          true",
        "true,             true,               false,        true,           false",
        "true,             true,               true,         false,          false",
        "true,             true,               true,         true,           false",
      ],
    )
    fun `returns the expected result for each combination of offender LAO status and user access`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      userExcluded: Boolean,
      userRestricted: Boolean,
      expectedCanAccess: Boolean,
    ) {
      val (user, _) = givenAUser()

      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      val crn = caseSummary.crn

      apDeliusContextCaseSummariesSingleCase(caseSummary)
      apDeliusContextUserAccessSingleCase(
        CaseAccessFactory()
          .withCrn(crn)
          .withUserExcluded(userExcluded)
          .withUserRestricted(userRestricted)
          .produce(),
        username = user.deliusUsername,
      )

      val canAccess = offenderService.canAccessOffender(
        crn = crn,
        laoStrategy = user.cas3LaoStrategy(),
      )

      assertThat(canAccess).isEqualTo(expectedCanAccess)

      apDeliusContextVerifyCaseSummariesCalls(times = 0)
      apDeliusContextVerifyUserAccessCalls(times = 1, username = user.deliusUsername)
    }
  }

  private fun assertFull(result: PersonSummaryInfoResult, caseSummary: CaseSummary) {
    assertThat(result).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
    result as PersonSummaryInfoResult.Success.Full
    assertThat(result.crn).isEqualTo(caseSummary.crn)
    assertThat(result.summary).isEqualTo(caseSummary)
  }

  private fun assertRestricted(result: PersonSummaryInfoResult, caseSummary: CaseSummary) {
    assertThat(result).isInstanceOf(PersonSummaryInfoResult.Success.Restricted::class.java)
    result as PersonSummaryInfoResult.Success.Restricted
    assertThat(result.crn).isEqualTo(caseSummary.crn)
    assertThat(result.nomsNumber).isEqualTo(caseSummary.nomsId)
  }

  private fun assertNotFound(result: PersonSummaryInfoResult, crn: String) {
    assertThat(result).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
    assertThat(result.crn).isEqualTo(crn)
  }
}
