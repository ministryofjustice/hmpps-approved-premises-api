package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspaceReasonTransformer

@ExtendWith(MockKExtension::class)
class Cas3VoidBedspacesReasonTransformerTest {

  private val cas3VoidBedspacesReasonTransformer: Cas3VoidBedspaceReasonTransformer =
    Cas3VoidBedspaceReasonTransformer()

  @Test
  fun `void bedspace reason is correctly transformed`() {
    val voidBedspaceReason =
      Cas3VoidBedspaceReasonEntityFactory()
        .withName("test name")
        .withIsActive(true)
        .produce()

    val transformed = cas3VoidBedspacesReasonTransformer.toCas3VoidBedspaceReason(voidBedspaceReason)
    assertAll(
      {
        assertThat(transformed.id).isEqualTo(voidBedspaceReason.id)
        assertThat(transformed.name).isEqualTo("test name")
        assertThat(transformed.isActive).isTrue
      },
    )
  }
}
