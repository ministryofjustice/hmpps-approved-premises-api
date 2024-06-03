package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedCancellationTransformer

class Cas1OutOfServiceBedCancellationTransformerTest {
  private val transformer = Cas1OutOfServiceBedCancellationTransformer()

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val cancellation = Cas1OutOfServiceBedCancellationEntityFactory()
      .withNotes("Some notes")
      .withOutOfServiceBed {
        withBed {
          withRoom {
            withPremises(
              ApprovedPremisesEntityFactory()
                .withDefaults()
                .produce(),
            )
          }
        }
      }
      .produce()

    val result = transformer.transformJpaToApi(cancellation)

    assertThat(result.id).isEqualTo(cancellation.id)
    assertThat(result.createdAt).isEqualTo(cancellation.createdAt.toInstant())
    assertThat(result.notes).isEqualTo(cancellation.notes)
  }
}
