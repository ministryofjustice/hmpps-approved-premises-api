package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

class Cas1OutOfServiceBedReasonTransformerTest {
  private val transformer = Cas1OutOfServiceBedReasonTransformer()

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val reason = Cas1OutOfServiceBedReasonEntityFactory()
      .produce()

    val result = transformer.transformJpaToApi(reason)

    assertThat(result.id).isEqualTo(reason.id)
    assertThat(result.name).isEqualTo(reason.name)
    assertThat(result.isActive).isEqualTo(reason.isActive)
  }
}
