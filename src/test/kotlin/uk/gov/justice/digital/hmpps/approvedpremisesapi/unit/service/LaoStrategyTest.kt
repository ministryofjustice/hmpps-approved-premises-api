package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy

class LaoStrategyTest {

  @Nested
  inner class Validation {

    @Test
    fun `constructor requires a username when checks are required`() {
      assertThatThrownBy {
        LaoStrategy(
          restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
          exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
          deliusUsername = null,
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Delius username is required when applying LAO checks")
    }

    @Test
    fun `constructor rejects a restriction check without an exclusion check`() {
      assertThatThrownBy {
        LaoStrategy(
          restrictionHandling = LaoStrategy.RestrictionHandling.ALWAYS_CHECK,
          exclusionHandling = LaoStrategy.ExclusionHandling.IGNORE,
          deliusUsername = "user1",
        )
      }.hasMessage("If restrictionHandling is ALWAYS_CHECK, exclusionHandling must also be ALWAYS_CHECK")
    }

    @Test
    fun `constructor rejects an exclusion check without a restriction check`() {
      assertThatThrownBy {
        LaoStrategy(
          restrictionHandling = LaoStrategy.RestrictionHandling.IGNORE,
          exclusionHandling = LaoStrategy.ExclusionHandling.ALWAYS_CHECK,
          deliusUsername = "user1",
        )
      }.hasMessage("If exclusionHandling is ALWAYS_CHECK, restrictionHandling must also be ALWAYS_CHECK")
    }
  }
}
