package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.unit.entity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory
import java.time.LocalDate

class RiskTierTest {
  @Test
  fun `Correctly unmarshals when no version defined, defaulting version to v2`() {
    val json = """
      {
        "level":"D2",
        "lastUpdated":[2024,1,5]
      }
    """.trimIndent()

    val unmarshalled = unmarshall(json)

    Assertions.assertThat(unmarshalled.level).isEqualTo("D2")
    Assertions.assertThat(unmarshalled.lastUpdated).isEqualTo(LocalDate.of(2024, 1, 5))
    Assertions.assertThat(unmarshalled.version).isEqualTo(RiskTierVersion.V2)
  }

  @Test
  fun `Correctly unmarshals when version defined`() {
    val json = """
      {
        "level":"D2",
        "lastUpdated":[2024,1,5],
        "version": "V3"
      }
    """.trimIndent()

    val unmarshalled = unmarshall(json)

    Assertions.assertThat(unmarshalled!!.level).isEqualTo("D2")
    Assertions.assertThat(unmarshalled.lastUpdated).isEqualTo(LocalDate.of(2024, 1, 5))
    Assertions.assertThat(unmarshalled.version).isEqualTo(RiskTierVersion.V3)
  }

  // hypersistence uses jackson 2
  private fun unmarshall(json: String) = JsonMapperFactory.createJackson2JsonMapper().readValue(json, RiskTier::class.java)
}
