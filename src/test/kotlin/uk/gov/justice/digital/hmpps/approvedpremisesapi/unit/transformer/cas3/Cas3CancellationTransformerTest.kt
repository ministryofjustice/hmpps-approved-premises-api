package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3CancellationTransformer

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
