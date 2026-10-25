package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy

class LaoStrategyTest {

  @Nested
  inner class Factories {

    @Test
    fun `checkUserAccess returns correct strategy`() {
      val result = LaoStrategy.checkUserAccess("the username")

      assertThat(result.deliusUsername).isEqualTo("the username")
      assertThat(result.restrictionHandling).isEqualTo(LaoStrategy.RestrictionHandling.ALWAYS_CHECK)
      assertThat(result.exclusionHandling).isEqualTo(LaoStrategy.ExclusionHandling.ALWAYS_CHECK)
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `constructor requires a username when restriction checks are required`() {
      assertThatThrownBy {
        LaoStrategy(
          restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
          exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
          deliusUsername = null,
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Delius username is required when applying LAO checks")
    }

    @Test
    fun `constructor requires a username when exclusion checks are required`() {
      assertThatThrownBy {
        LaoStrategy(
          restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
          exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
          deliusUsername = null,
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Delius username is required when applying LAO checks")
    }
  }

  @Nested
  inner class IgnoreAll {

    @Test
    fun `returns true when both checks are ignored`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = null,
      )

      assertThat(strategy.ignoreAll()).isTrue()
    }

    @Test
    fun `returns false when restrictions are checked`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      assertThat(strategy.ignoreAll()).isFalse()
    }

    @Test
    fun `returns false when exclusions are checked`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = "user1",
      )

      assertThat(strategy.ignoreAll()).isFalse()
    }
  }

  @Nested
  inner class CanViewUsingCaseSummary {

    @Test
    fun `returns CAN_VIEW when checks are enabled but neither restriction nor exclusion is present`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseSummaryFactory()
          .withCurrentRestriction(currentRestriction = false)
          .withCurrentExclusion(currentExclusion = false)
          .produce(),
      )

      assertThat(result).isEqualTo(LaoStrategy.CaseSummaryViewResult.CAN_VIEW)
    }

    @Test
    fun `returns CHECK_CASE_ACCESS when restriction checks are enabled and a restriction is present`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseSummaryFactory()
          .withCurrentRestriction(currentRestriction = true)
          .withCurrentExclusion(currentExclusion = false)
          .produce(),
      )

      assertThat(result)
        .isEqualTo(LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS)
    }

    @Test
    fun `returns CHECK_CASE_ACCESS when exclusions checks are enabled and an exclusion is present`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseSummaryFactory()
          .withCurrentRestriction(currentRestriction = false)
          .withCurrentExclusion(currentExclusion = true)
          .produce(),
      )

      assertThat(result)
        .isEqualTo(LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS)
    }

    @Test
    fun `returns CHECK_CASE_ACCESS when all checks are enabled and a restriction and exclusion are present`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseSummaryFactory()
          .withCurrentRestriction(currentRestriction = true)
          .withCurrentExclusion(currentExclusion = true)
          .produce(),
      )

      assertThat(result)
        .isEqualTo(LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS)
    }

    @Test
    fun `returns CAN_VIEW when checks are ignored even if the case summary has restrictions and exclusions`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = null,
      )

      val result = strategy.canView(
        CaseSummaryFactory()
          .withCurrentRestriction(currentRestriction = true)
          .withCurrentExclusion(currentExclusion = true)
          .produce(),
      )

      assertThat(result)
        .isEqualTo(LaoStrategy.CaseSummaryViewResult.CAN_VIEW)
    }
  }

  @Nested
  inner class CanViewCaseAccess {

    @Test
    fun `returns true when checks are enabled and the user is neither excluded nor restricted`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(false)
          .withUserExcluded(false)
          .produce(),
      )

      assertThat(result).isTrue()
    }

    @Test
    fun `returns false when restriction checks are enabled and the user is restricted`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(true)
          .withUserExcluded(false)
          .produce(),
      )

      assertThat(result).isFalse()
    }

    @Test
    fun `returns false when exclusion checks are enabled and the user is excluded`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(false)
          .withUserExcluded(true)
          .produce(),
      )

      assertThat(result).isFalse()
    }

    @Test
    fun `returns false when all checks are enabled and the user is restricted and excluded`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(true)
          .withUserExcluded(true)
          .produce(),
      )

      assertThat(result).isFalse()
    }

    @Test
    fun `returns true when restrictions checks are ignored and restriction exists`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(true)
          .withUserExcluded(false)
          .produce(),
      )

      assertThat(result).isTrue()
    }

    @Test
    fun `returns true when exclusion checks are ignored and exclusion exists`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = "user1",
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(false)
          .withUserExcluded(true)
          .produce(),
      )

      assertThat(result).isTrue()
    }

    @Test
    fun `returns true when all checks are ignored even if the user is both restricted and excluded`() {
      val strategy = LaoStrategy(
        restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
        exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
        deliusUsername = null,
      )

      val result = strategy.canView(
        CaseAccessFactory()
          .withUserRestricted(true)
          .withUserExcluded(true)
          .produce(),
      )

      assertThat(result).isTrue()
    }
  }
}
