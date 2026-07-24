package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1BedSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedSummaryTransformer

class Cas1BedSummaryTransformerTest {
  private val bedSummaryTransformer = Cas1BedSummaryTransformer()

  @Test
  fun `it transforms an available bed correctly`() {
    val domainSummary = Cas1BedSummaryFactory()
      .withBedBooked(false)
      .withBedOutOfService(false)
      .produce()

    val summary = bedSummaryTransformer.transformToApi(domainSummary)

    assertThat(summary.id).isEqualTo(domainSummary.id)
    assertThat(summary.name).isEqualTo(domainSummary.name)
    assertThat(summary.roomName).isEqualTo(domainSummary.roomName)
    assertThat(summary.status).isEqualTo(BedStatus.available)
  }

  @Test
  fun `it transforms a booked bed correctly`() {
    val domainSummary = Cas1BedSummaryFactory()
      .withBedBooked(true)
      .withBedOutOfService(false)
      .produce()

    val summary = bedSummaryTransformer.transformToApi(domainSummary)

    assertThat(summary.id).isEqualTo(domainSummary.id)
    assertThat(summary.name).isEqualTo(domainSummary.name)
    assertThat(summary.roomName).isEqualTo(domainSummary.roomName)
    assertThat(summary.status).isEqualTo(BedStatus.occupied)
  }

  @Test
  fun `it transforms an unavailable bed correctly`() {
    val domainSummary = Cas1BedSummaryFactory()
      .withBedBooked(false)
      .withBedOutOfService(true)
      .produce()

    val summary = bedSummaryTransformer.transformToApi(domainSummary)

    assertThat(summary.id).isEqualTo(domainSummary.id)
    assertThat(summary.name).isEqualTo(domainSummary.name)
    assertThat(summary.roomName).isEqualTo(domainSummary.roomName)
    assertThat(summary.status).isEqualTo(BedStatus.outOfService)
  }
}
