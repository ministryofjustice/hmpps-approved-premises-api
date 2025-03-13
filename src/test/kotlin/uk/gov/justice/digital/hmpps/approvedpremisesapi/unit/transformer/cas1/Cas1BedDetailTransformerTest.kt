package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedDetailTransformer

class Cas1BedDetailTransformerTest {
  private val bedSummaryTransformer = mockk<BedSummaryTransformer>()
  private val cas1BedDetailTransformer = Cas1BedDetailTransformer(bedSummaryTransformer)

  @Test
  fun `transformToApi transforms correctly`() {
    val domainSummary = BedSummaryFactory()
      .withName("bed name")
      .produce()

    every { bedSummaryTransformer.transformToApi(any()) } returns BedSummary(
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

  private fun getStatus(summary: DomainBedSummary): BedStatus {
    if (summary.bedBooked) {
      return BedStatus.occupied
    } else if (summary.bedOutOfService) {
      return BedStatus.outOfService
    } else {
      return BedStatus.available
    }
  }
}
