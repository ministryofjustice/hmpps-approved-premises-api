package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.RiskEnvelopeStatusDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import java.time.LocalDate

class RisksTransformerTest {

  @CsvSource(
    "V2,V2",
    "V3,V3",
  )
  @ParameterizedTest
  fun `tier transform`(version: RiskTierVersion, expectedVersion: TierVersionDto) {
    val domain = RiskWithStatus(
      status = RiskStatus.Retrieved,
      value = RiskTier(
        level = "A",
        lastUpdated = LocalDate.of(2021, 7, 5),
        version = version,
      ),
    )

    val result = RisksTransformer().transformTierDomainToApi(domain)

    assertThat(result.status).isEqualTo(RiskEnvelopeStatusDto.retrieved)
    assertThat(result.value?.level).isEqualTo("A")
    assertThat(result.value?.lastUpdated).isEqualTo(LocalDate.of(2021, 7, 5))
    assertThat(result.value?.version).isEqualTo(expectedVersion)
  }
}
