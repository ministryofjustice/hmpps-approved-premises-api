package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer

class Cas3CancellationTransformerTest {

  private val transformer = Cas3CancellationTransformer(CancellationReasonTransformer())

  @Test
  fun transformJpaToApi() {
    val result = transformer.transformJpaToApi(
      Cas3CancellationEntityFactory()
        .withDefaults()
        .withBooking(Cas3BookingEntityFactory().withDefaults().produce())
        .withOtherReason("the other reason")
        .produce(),
    )

    assertThat(result!!.otherReason).isEqualTo("the other reason")
  }
}
