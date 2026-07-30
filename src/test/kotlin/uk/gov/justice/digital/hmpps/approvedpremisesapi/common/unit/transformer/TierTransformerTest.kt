package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.transformer.toEventTier
import java.time.LocalDateTime

class TierTransformerTest {

  @Test
  fun `toEventTier maps all fields to the domain event EventTier`() {
    val calculationDate = LocalDateTime.parse("2026-07-30T11:59:34")

    val result = TierDto(
      tierScore = "A3",
      calculationDate = calculationDate,
      provisional = true,
      version = TierVersionDto.V3,
    ).toEventTier()

    assertThat(result).isEqualTo(
      EventTier(
        tierScore = "A3",
        calculationDate = calculationDate,
        provisional = true,
        version = EventTierVersion.V3,
      ),
    )
  }

  @Test
  fun `toEventTier preserves a null provisional value`() {
    val result = TierDto(
      tierScore = "B1",
      calculationDate = LocalDateTime.parse("2026-07-30T11:59:34"),
      provisional = null,
      version = TierVersionDto.V2,
    ).toEventTier()

    assertThat(result.provisional).isNull()
  }

  @ParameterizedTest
  @EnumSource(TierVersionDto::class)
  fun `toEventTier maps each TierVersionDto to the matching EventTierVersion`(version: TierVersionDto) {
    val result = TierDto(
      tierScore = "A1",
      calculationDate = LocalDateTime.parse("2026-07-30T11:59:34"),
      provisional = false,
      version = version,
    ).toEventTier()

    assertThat(result.version).isEqualTo(EventTierVersion.valueOf(version.name))
  }
}
