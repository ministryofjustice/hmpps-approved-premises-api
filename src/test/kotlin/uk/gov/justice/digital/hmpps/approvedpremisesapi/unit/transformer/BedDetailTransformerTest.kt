package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer

class BedDetailTransformerTest {
  private val bedSummaryTransformer = BedSummaryTransformer()
  private val bedDetailTransformer = BedDetailTransformer(bedSummaryTransformer)

  @Test
  fun `it transforms the bed and characteristics`() {
    val domainSummary = BedSummaryFactory()
      .produce()

    val characteristics = listOf(
      CharacteristicEntityFactory().produce(),
      CharacteristicEntityFactory().produce(),
    )

    val result = bedDetailTransformer.transformToApi(Pair(domainSummary, characteristics))

    assertThat(result.id).isEqualTo(domainSummary.id)
    assertThat(result.name).isEqualTo(domainSummary.name)
    assertThat(result.roomName).isEqualTo(domainSummary.roomName)
    assertThat(result.status).isEqualTo(BedStatus.AVAILABLE)

    assertThat(result.characteristics.size).isEqualTo(2)

    assertThat(result.characteristics[0]).isEqualTo(
      CharacteristicPair(characteristics[0].name, characteristics[0].propertyName),
    )
    assertThat(result.characteristics[1]).isEqualTo(
      CharacteristicPair(characteristics[1].name, characteristics[1].propertyName),
    )
  }
}
