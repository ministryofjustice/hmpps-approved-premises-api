package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CsraSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundCsraSummariesCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulCsraSummariesCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIVerifyCsraSummariesCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

/**
 * These tests validate the LaoStrategies used by CAS1 by calling OffenderServices directly
 *
 * This has been added because testing all LAO permutations for all CAS1 endpoints that may
 * be impacted by LAO is impractical
 *
 * CAS1 uses a different [LaoStrategy] depending on whether the user has the [UserQualification.LAO]
 * qualification (see [cas1LaoStrategy]), so each strategy is tested separately
 */
class Cas1LaoIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var offenderService: OffenderService

  enum class ExpectedResult { FULL, RESTRICTED }

  enum class ExpectedCsraResult { SUCCESS, UNAUTHORISED }

  @Nested
  inner class NeverRestrictedStrategy {

    private fun givenAUserWithNeverRestrictedStrategy(): UserEntity {
      val (user, _) = givenAUser(qualifications = listOf(UserQualification.LAO))
      assertThat(user.cas1LaoStrategy()).isEqualTo(LaoStrategy.NeverRestricted)
      return user
    }

    @Nested
    inner class GetPersonSummaryInfoResults {

      @ParameterizedTest(
        name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> {4} (access checked={5})",
      )
      @CsvSource(
        // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedResult, userAccessChecked
        value = [
          "false,            false,              false,        false,          FULL,           false",
          "false,            true,               false,        false,          FULL,           false",
          "false,            true,               false,        true,           FULL,           false",
          "true,             false,              false,        false,          FULL,           false",
          "true,             false,              true,         false,          FULL,           false",
          "true,             true,               false,        false,          FULL,           false",
          "true,             true,               false,        true,           FULL,           false",
          "true,             true,               true,         false,          FULL,           false",
          "true,             true,               true,         true,           FULL,           false",
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
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)

        val results = offenderService.getPersonSummaryInfoResults(
          crns = setOf(caseSummary.crn),
          laoStrategy = user.cas1LaoStrategy(),
          includeTier = false,
        )

        assertThat(results).hasSize(1)
        assertResult(results.first(), caseSummary, expectedResult)

        val expectedUserAccessCalls = if (userAccessChecked) 1 else 0
        apDeliusContextVerifyUserAccessCalls(times = expectedUserAccessCalls)
      }

      @Test
      fun `returns NotFound when the CRN does not exist and does not check user access`() {
        val user = givenAUserWithNeverRestrictedStrategy()
        val crn = randomStringUpperCase(8)

        apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

        val results = offenderService.getPersonSummaryInfoResults(
          crns = setOf(crn),
          laoStrategy = user.cas1LaoStrategy(),
          includeTier = false,
        )

        assertThat(results).hasSize(1)
        assertNotFound(results.first(), crn)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns Full when the offender is LAO and no user access information is available`() {
        val user = givenAUserWithNeverRestrictedStrategy()

        val caseSummary = CaseSummaryFactory()
          .withCurrentRestriction(true)
          .produce()

        apDeliusContextCaseSummariesSingleCase(caseSummary)

        val results = offenderService.getPersonSummaryInfoResults(
          crns = setOf(caseSummary.crn),
          laoStrategy = user.cas1LaoStrategy(),
          includeTier = false,
        )

        assertThat(results).hasSize(1)
        assertFull(results.first(), caseSummary)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns empty list and makes no upstream calls when no CRNs are provided`() {
        val user = givenAUserWithNeverRestrictedStrategy()

        val results = offenderService.getPersonSummaryInfoResults(
          crns = emptySet(),
          laoStrategy = user.cas1LaoStrategy(),
        )

        assertThat(results).isEmpty()
        apDeliusContextVerifyCaseSummariesCalls(times = 0)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns the expected result for each CRN when requesting multiple CRNs with mixed LAO status and user access`() {
        val user = givenAUserWithNeverRestrictedStrategy()

        val notLao = CaseSummaryFactory().withNoLimitedAccess().produce()
        val excludedUserExcluded = CaseSummaryFactory().withCurrentExclusion(true).produce()
        val restrictedUserRestricted = CaseSummaryFactory().withCurrentRestriction(true).produce()
        val excludedAndRestrictedUserExcludedAndRestricted = CaseSummaryFactory()
          .withCurrentExclusion(true)
          .withCurrentRestriction(true)
          .produce()
        val notFoundCrn = randomStringUpperCase(8)

        val caseSummaries = listOf(
          notLao,
          excludedUserExcluded,
          restrictedUserRestricted,
          excludedAndRestrictedUserExcludedAndRestricted,
        )
        val crns = caseSummaries.map { it.crn } + notFoundCrn

        apDeliusContextCaseSummariesMultipleCases(casesSummary = caseSummaries, crns = crns)
        apDeliusContextUserAccessAddCase(
          listOf(
            CaseAccessFactory().withCrn(excludedUserExcluded.crn).withUserExcluded(true).produce(),
            CaseAccessFactory().withCrn(restrictedUserRestricted.crn).withUserRestricted(true).produce(),
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
          laoStrategy = user.cas1LaoStrategy(),
          includeTier = false,
        )

        assertThat(results).hasSize(crns.size)
        assertFull(results.forCrn(notLao.crn), notLao)
        assertFull(results.forCrn(excludedUserExcluded.crn), excludedUserExcluded)
        assertFull(results.forCrn(restrictedUserRestricted.crn), restrictedUserRestricted)
        assertFull(
          results.forCrn(excludedAndRestrictedUserExcludedAndRestricted.crn),
          excludedAndRestrictedUserExcludedAndRestricted,
        )
        assertNotFound(results.forCrn(notFoundCrn), notFoundCrn)

        apDeliusContextVerifyUserAccessCalls(times = 0)
      }
    }

    @Nested
    inner class CanAccessOffender {

      @ParameterizedTest(
        name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> canAccess={4} (access checked={5})",
      )
      @CsvSource(
        // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedCanAccess, userAccessChecked
        value = [
          "false,            false,              false,        false,          true,              false",
          "false,            true,               false,        false,          true,              false",
          "false,            true,               false,        true,           true,              false",
          "true,             false,              false,        false,          true,              false",
          "true,             false,              true,         false,          true,              false",
          "true,             true,               false,        false,          true,              false",
          "true,             true,               false,        true,           true,              false",
          "true,             true,               true,         false,          true,              false",
          "true,             true,               true,         true,           true,              false",
        ],
      )
      @SuppressWarnings("LongParameterList")
      fun `returns the expected result for each combination of offender LAO status and user access`(
        currentExclusion: Boolean,
        currentRestriction: Boolean,
        userExcluded: Boolean,
        userRestricted: Boolean,
        expectedCanAccess: Boolean,
        userAccessChecked: Boolean,
      ) {
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)

        val canAccess = offenderService.canAccessOffender(
          crn = caseSummary.crn,
          laoStrategy = user.cas1LaoStrategy(),
        )

        assertThat(canAccess).isEqualTo(expectedCanAccess)

        val expectedUserAccessCalls = if (userAccessChecked) 1 else 0
        apDeliusContextVerifyCaseSummariesCalls(times = 0)
        apDeliusContextVerifyUserAccessCalls(times = expectedUserAccessCalls)
      }
    }

    @Nested
    inner class GetCsraSummariesForOffender {

      @ParameterizedTest(
        name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> {4}",
      )
      @CsvSource(
        // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedResult
        value = [
          "false,            false,              false,        false,          SUCCESS",
          "false,            true,               false,        false,          SUCCESS",
          "false,            true,               false,        true,           SUCCESS",
          "true,             false,              false,        false,          SUCCESS",
          "true,             false,              true,         false,          SUCCESS",
          "true,             true,               false,        false,          SUCCESS",
          "true,             true,               false,        true,           SUCCESS",
          "true,             true,               true,         false,          SUCCESS",
          "true,             true,               true,         true,           SUCCESS",
        ],
      )
      @SuppressWarnings("LongParameterList")
      fun `when the case is held locally, returns the expected result without any delius calls`(
        currentExclusion: Boolean,
        currentRestriction: Boolean,
        userExcluded: Boolean,
        userRestricted: Boolean,
        expectedResult: ExpectedCsraResult,
      ) {
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)
        givenALocalCase(caseSummary)
        val csraSummaries = givenCsraSummaries(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertCsraResult(result, expectedResult, csraSummaries)
        prisonAPIVerifyCsraSummariesCalls(caseSummary.nomsId!!, times = 1)
        apDeliusContextVerifyCaseSummariesCalls(times = 0)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @ParameterizedTest(name = "currentExclusion={0}, currentRestriction={1} -> {2}")
      @CsvSource(
        // currentExclusion, currentRestriction, expectedResult
        value = [
          "false,            false,              SUCCESS",
          "false,            true,               SUCCESS",
          "true,             false,              SUCCESS",
          "true,             true,               SUCCESS",
        ],
      )
      fun `when the case is not held locally, falls back to the delius case summary and ignores LAO status`(
        currentExclusion: Boolean,
        currentRestriction: Boolean,
        expectedResult: ExpectedCsraResult,
      ) {
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = CaseSummaryFactory()
          .withCurrentExclusion(currentExclusion)
          .withCurrentRestriction(currentRestriction)
          .produce()
        apDeliusContextCaseSummariesSingleCase(caseSummary)
        val csraSummaries = givenCsraSummaries(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertCsraResult(result, expectedResult, csraSummaries)
        prisonAPIVerifyCsraSummariesCalls(caseSummary.nomsId!!, times = 1)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `when the local case has no noms number, falls back to the delius case summary`() {
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = CaseSummaryFactory().withCurrentRestriction(true).produce()
        caseEntityFactory.produceAndPersist {
          withCrn(caseSummary.crn)
          withNomsNumber(null)
        }
        apDeliusContextCaseSummariesSingleCase(caseSummary)
        val csraSummaries = givenCsraSummaries(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertCsraResult(result, ExpectedCsraResult.SUCCESS, csraSummaries)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns NotFound for the noms number when the delius case summary has no noms number`() {
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = CaseSummaryFactory().withNomsId(null).withCurrentRestriction(true).produce()
        apDeliusContextCaseSummariesSingleCase(caseSummary)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertThat(result).isEqualTo(CasResult.NotFound<List<CsraSummary>>("Noms Number", caseSummary.crn))
      }

      @Test
      fun `returns NotFound for the person when the CRN is not held locally or in delius`() {
        val user = givenAUserWithNeverRestrictedStrategy()
        val crn = randomStringUpperCase(8)
        apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

        val result = offenderService.getCsraSummariesForOffender(crn, user.cas1LaoStrategy())

        assertThat(result).isEqualTo(CasResult.NotFound<List<CsraSummary>>("Person", crn))
      }

      @Test
      fun `returns NotFound for the CSRA summary when the prison API has no record`() {
        val user = givenAUserWithNeverRestrictedStrategy()
        val caseSummary = CaseSummaryFactory().withCurrentRestriction(false).withCurrentExclusion(false).produce()
        apDeliusContextCaseSummariesSingleCase(caseSummary)
        givenALocalCase(caseSummary)

        prisonAPIMockNotFoundCsraSummariesCall(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertThat(result).isEqualTo(CasResult.NotFound<List<CsraSummary>>("CsraSummary", caseSummary.crn))
      }
    }
  }

  @Nested
  inner class CheckUserAccessStrategy {

    private fun givenAUserWithCheckUserAccessStrategy(): UserEntity {
      val (user, _) = givenAUser()
      assertThat(user.cas1LaoStrategy()).isEqualTo(LaoStrategy.checkUserAccess(user.deliusUsername))
      return user
    }

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
        val user = givenAUserWithCheckUserAccessStrategy()
        val caseSummary = givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)

        val results = offenderService.getPersonSummaryInfoResults(
          crns = setOf(caseSummary.crn),
          laoStrategy = user.cas1LaoStrategy(),
          includeTier = false,
        )

        assertThat(results).hasSize(1)
        assertResult(results.first(), caseSummary, expectedResult)

        val expectedUserAccessCalls = if (userAccessChecked) 1 else 0
        apDeliusContextVerifyUserAccessCalls(times = expectedUserAccessCalls, username = user.deliusUsername)
      }

      @Test
      fun `returns NotFound when the CRN does not exist and does not check user access`() {
        val user = givenAUserWithCheckUserAccessStrategy()
        val crn = randomStringUpperCase(8)

        apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

        val results = offenderService.getPersonSummaryInfoResults(
          crns = setOf(crn),
          laoStrategy = user.cas1LaoStrategy(),
          includeTier = false,
        )

        assertThat(results).hasSize(1)
        assertNotFound(results.first(), crn)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns empty list and makes no upstream calls when no CRNs are provided`() {
        val user = givenAUserWithCheckUserAccessStrategy()

        val results = offenderService.getPersonSummaryInfoResults(
          crns = emptySet(),
          laoStrategy = user.cas1LaoStrategy(),
        )

        assertThat(results).isEmpty()
        apDeliusContextVerifyCaseSummariesCalls(times = 0)
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns the expected result for each CRN when requesting multiple CRNs with mixed LAO status and user access`() {
        val user = givenAUserWithCheckUserAccessStrategy()

        val notLao = CaseSummaryFactory().withNoLimitedAccess().produce()
        val excludedUserExcluded = CaseSummaryFactory().withCurrentExclusion(true).produce()
        val excludedUserHasAccess = CaseSummaryFactory().withCurrentExclusion(true).produce()
        val restrictedUserRestricted = CaseSummaryFactory().withCurrentRestriction(true).produce()
        val restrictedUserHasAccess = CaseSummaryFactory().withCurrentRestriction(true).produce()
        val excludedAndRestrictedUserExcludedAndRestricted = CaseSummaryFactory()
          .withCurrentExclusion(true)
          .withCurrentRestriction(true)
          .produce()
        val notFoundCrn = randomStringUpperCase(8)

        val caseSummaries = listOf(
          notLao,
          excludedUserExcluded,
          excludedUserHasAccess,
          restrictedUserRestricted,
          restrictedUserHasAccess,
          excludedAndRestrictedUserExcludedAndRestricted,
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
          laoStrategy = user.cas1LaoStrategy(),
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
        assertNotFound(results.forCrn(notFoundCrn), notFoundCrn)

        apDeliusContextVerifyUserAccessCalls(times = 1, username = user.deliusUsername)
      }
    }

    @Nested
    inner class CanAccessOffender {

      @ParameterizedTest(
        name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> canAccess={4} (access checked={5})",
      )
      @CsvSource(
        // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedCanAccess, userAccessChecked
        value = [
          "false,            false,              false,        false,          true,              true",
          "false,            true,               false,        false,          true,              true",
          "false,            true,               false,        true,           false,             true",
          "true,             false,              false,        false,          true,              true",
          "true,             false,              true,         false,          false,             true",
          "true,             true,               false,        false,          true,              true",
          "true,             true,               false,        true,           false,             true",
          "true,             true,               true,         false,          false,             true",
          "true,             true,               true,         true,           false,             true",
        ],
      )
      @SuppressWarnings("LongParameterList")
      fun `returns the expected result for each combination of offender LAO status and user access`(
        currentExclusion: Boolean,
        currentRestriction: Boolean,
        userExcluded: Boolean,
        userRestricted: Boolean,
        expectedCanAccess: Boolean,
        userAccessChecked: Boolean,
      ) {
        val user = givenAUserWithCheckUserAccessStrategy()
        val caseSummary = givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)

        val canAccess = offenderService.canAccessOffender(
          crn = caseSummary.crn,
          laoStrategy = user.cas1LaoStrategy(),
        )

        assertThat(canAccess).isEqualTo(expectedCanAccess)

        val expectedUserAccessCalls = if (userAccessChecked) 1 else 0
        apDeliusContextVerifyCaseSummariesCalls(times = 0)
        apDeliusContextVerifyUserAccessCalls(times = expectedUserAccessCalls, username = user.deliusUsername)
      }
    }

    @Nested
    inner class GetCsraSummariesForOffender {

      @ParameterizedTest(
        name = "currentExclusion={0}, currentRestriction={1}, userExcluded={2}, userRestricted={3} -> {4} (access checked={5})",
      )
      @CsvSource(
        // currentExclusion, currentRestriction, userExcluded, userRestricted, expectedResult, userAccessChecked
        value = [
          "false,            false,              false,        false,          SUCCESS,        false",
          "false,            true,               false,        false,          SUCCESS,        true",
          "false,            true,               false,        true,           UNAUTHORISED,   true",
          "true,             false,              false,        false,          SUCCESS,        true",
          "true,             false,              true,         false,          UNAUTHORISED,   true",
          "true,             true,               false,        false,          SUCCESS,        true",
          "true,             true,               false,        true,           UNAUTHORISED,   true",
          "true,             true,               true,         false,          UNAUTHORISED,   true",
          "true,             true,               true,         true,           UNAUTHORISED,   true",
        ],
      )
      @SuppressWarnings("LongParameterList")
      fun `returns the expected result for each combination of offender LAO status and user access`(
        currentExclusion: Boolean,
        currentRestriction: Boolean,
        userExcluded: Boolean,
        userRestricted: Boolean,
        expectedResult: ExpectedCsraResult,
        userAccessChecked: Boolean,
      ) {
        val user = givenAUserWithCheckUserAccessStrategy()
        val caseSummary = givenAnOffenderWithAccess(user, currentExclusion, currentRestriction, userExcluded, userRestricted)
        val csraSummaries = givenCsraSummaries(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertCsraResult(result, expectedResult, csraSummaries)

        val expectedPrisonApiCalls = if (expectedResult == ExpectedCsraResult.SUCCESS) 1 else 0
        val expectedUserAccessCalls = if (userAccessChecked) 1 else 0
        prisonAPIVerifyCsraSummariesCalls(caseSummary.nomsId!!, times = expectedPrisonApiCalls)
        apDeliusContextVerifyUserAccessCalls(times = expectedUserAccessCalls, username = user.deliusUsername)
      }

      @Test
      fun `ignores any locally held case and still checks user access`() {
        val user = givenAUserWithCheckUserAccessStrategy()
        val caseSummary = givenAnOffenderWithAccess(
          user,
          currentExclusion = true,
          currentRestriction = false,
          userExcluded = true,
          userRestricted = false,
        )
        givenALocalCase(caseSummary)
        givenCsraSummaries(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertCsraResult(result, ExpectedCsraResult.UNAUTHORISED)
        prisonAPIVerifyCsraSummariesCalls(caseSummary.nomsId!!, times = 0)
        apDeliusContextVerifyUserAccessCalls(times = 1, username = user.deliusUsername)
      }

      @Test
      fun `returns NotFound for the noms number when the offender has no noms number`() {
        val user = givenAUserWithCheckUserAccessStrategy()
        val caseSummary = CaseSummaryFactory().withNomsId(null).produce()
        apDeliusContextCaseSummariesSingleCase(caseSummary)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertThat(result).isEqualTo(CasResult.NotFound<List<CsraSummary>>("Noms Number", caseSummary.crn))
      }

      @Test
      fun `returns NotFound for the person when the CRN does not exist and does not check user access`() {
        val user = givenAUserWithCheckUserAccessStrategy()
        val crn = randomStringUpperCase(8)
        apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

        val result = offenderService.getCsraSummariesForOffender(crn, user.cas1LaoStrategy())

        assertThat(result).isEqualTo(CasResult.NotFound<List<CsraSummary>>("Person", crn))
        apDeliusContextVerifyUserAccessCalls(times = 0)
      }

      @Test
      fun `returns NotFound for the CSRA summary when the prison API has no record`() {
        val user = givenAUserWithCheckUserAccessStrategy()
        val caseSummary = givenAnOffenderWithAccess(
          user,
          currentExclusion = false,
          currentRestriction = false,
          userExcluded = false,
          userRestricted = false,
        )
        prisonAPIMockNotFoundCsraSummariesCall(caseSummary.nomsId!!)

        val result = offenderService.getCsraSummariesForOffender(caseSummary.crn, user.cas1LaoStrategy())

        assertThat(result).isEqualTo(CasResult.NotFound<List<CsraSummary>>("CsraSummary", caseSummary.crn))
      }
    }
  }

  private fun givenALocalCase(caseSummary: CaseSummary) = caseEntityFactory.produceAndPersist {
    withCrn(caseSummary.crn)
    withNomsNumber(caseSummary.nomsId)
  }

  private fun givenCsraSummaries(nomsNumber: String): List<CsraSummary> {
    val csraSummaries = listOf(
      CsraSummary(
        bookingId = 123,
        classificationCode = null,
        assessmentCode = "CSR",
        assessmentSeq = 1,
        assessmentDate = LocalDate.now(),
        assessmentAgencyId = null,
        assessmentComment = "comment",
        cellSharingAlertFlag = true,
      ),
    )
    prisonAPIMockSuccessfulCsraSummariesCall(nomsNumber, csraSummaries)
    return csraSummaries
  }

  private fun assertCsraResult(
    result: CasResult<List<CsraSummary>>,
    expectedResult: ExpectedCsraResult,
    csraSummaries: List<CsraSummary> = emptyList(),
  ) = when (expectedResult) {
    ExpectedCsraResult.SUCCESS -> assertThat(result).isEqualTo(CasResult.Success(csraSummaries))
    ExpectedCsraResult.UNAUTHORISED -> assertThat(result).isInstanceOf(CasResult.Unauthorised::class.java)
  }

  @SuppressWarnings("LongParameterList")
  private fun givenAnOffenderWithAccess(
    user: UserEntity,
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
      username = user.deliusUsername,
    )

    return caseSummary
  }

  private fun assertResult(result: PersonSummaryInfoResult, caseSummary: CaseSummary, expectedResult: ExpectedResult) = when (expectedResult) {
    ExpectedResult.FULL -> assertFull(result, caseSummary)
    ExpectedResult.RESTRICTED -> assertRestricted(result, caseSummary)
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
