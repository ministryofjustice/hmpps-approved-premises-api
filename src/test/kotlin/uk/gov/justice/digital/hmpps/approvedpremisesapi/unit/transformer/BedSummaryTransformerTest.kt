package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer

class BedSummaryTransformerTest {
  private val bedSummaryTransformer = BedSummaryTransformer()

  @Test
  fun `it transforms an available bed correctly`() {
    val domainSummary = BedSummaryFactory()
      .withBedBooked(false)
      .withBedOutOfService(false)
      .produce()

    val summary = bedSummaryTransformer.transformToApi(domainSummary)

    assertThat(summary.id).isEqualTo(domainSummary.id)
    assertThat(summary.name).isEqualTo(domainSummary.name)
    assertThat(summary.roomName).isEqualTo(domainSummary.roomName)
    assertThat(summary.status).isEqualTo(BedStatus.AVAILABLE)
  }

  @Test
  fun `it transforms a booked bed correctly`() {
    val domainSummary = BedSummaryFactory()
      .withBedBooked(true)
      .withBedOutOfService(false)
      .produce()

    val summary = bedSummaryTransformer.transformToApi(domainSummary)

    assertThat(summary.id).isEqualTo(domainSummary.id)
    assertThat(summary.name).isEqualTo(domainSummary.name)
    assertThat(summary.roomName).isEqualTo(domainSummary.roomName)
    assertThat(summary.status).isEqualTo(BedStatus.OCCUPIED)
  }

  @Test
  fun `it transforms an unavailable bed correctly`() {
    val domainSummary = BedSummaryFactory()
      .withBedBooked(false)
      .withBedOutOfService(true)
      .produce()

    val summary = bedSummaryTransformer.transformToApi(domainSummary)

    assertThat(summary.id).isEqualTo(domainSummary.id)
    assertThat(summary.name).isEqualTo(domainSummary.name)
    assertThat(summary.roomName).isEqualTo(domainSummary.roomName)
    assertThat(summary.status).isEqualTo(BedStatus.OUT_OF_SERVICE)
  }
}
