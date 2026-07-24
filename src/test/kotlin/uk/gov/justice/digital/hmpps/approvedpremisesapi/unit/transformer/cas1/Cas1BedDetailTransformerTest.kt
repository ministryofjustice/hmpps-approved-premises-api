package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1BedSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedSummaryTransformer

class Cas1BedDetailTransformerTest {
  private val cas1BedSummaryTransformer = mockk<Cas1BedSummaryTransformer>()
  private val cas1BedDetailTransformer = Cas1BedDetailTransformer(cas1BedSummaryTransformer)

  @Test
  fun `transformToApi transforms correctly`() {
    val domainSummary = Cas1BedSummaryFactory()
      .withName("bed name")
      .produce()

    every { cas1BedSummaryTransformer.transformToApi(any()) } returns BedSummary(
      id = domainSummary.id,
      name = domainSummary.name,
      roomName = domainSummary.roomName,
      status = getStatus(domainSummary),
    )

    val result = cas1BedDetailTransformer.transformToApi(
      Pair(
        domainSummary,
        listOf(
          CharacteristicEntityFactory().withPropertyName("arsonOffences").produce(),
          CharacteristicEntityFactory().withPropertyName("hasWheelChairAccessibleBathrooms").produce(),
        ),
      ),
    )

    Assertions.assertThat(result.name).isEqualTo("bed name")
    Assertions.assertThat(result.characteristics).containsExactlyInAnyOrder(Cas1SpaceCharacteristic.arsonOffences, Cas1SpaceCharacteristic.hasWheelChairAccessibleBathrooms)
  }

  private fun getStatus(summary: Cas1DomainBedSummary): BedStatus {
    if (summary.bedBooked) {
      return BedStatus.occupied
    } else if (summary.bedOutOfService) {
      return BedStatus.outOfService
    } else {
      return BedStatus.available
    }
  }
}
