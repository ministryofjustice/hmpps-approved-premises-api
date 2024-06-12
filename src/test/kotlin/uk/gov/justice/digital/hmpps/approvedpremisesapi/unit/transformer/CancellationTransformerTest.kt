package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer

class CancellationTransformerTest {

  private val transformer = CancellationTransformer(CancellationReasonTransformer())

  @Test
  fun transformJpaToApi() {
    val result = transformer.transformJpaToApi(
      CancellationEntityFactory()
        .withDefaults()
        .withBooking(BookingEntityFactory().withDefaults().produce())
        .withOtherReason("the other reason")
        .produce(),
    )

    assertThat(result!!.otherReason).isEqualTo("the other reason")
  }
}
