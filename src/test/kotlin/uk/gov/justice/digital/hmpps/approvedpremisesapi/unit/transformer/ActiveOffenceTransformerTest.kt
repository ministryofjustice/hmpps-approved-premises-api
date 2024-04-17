package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailOffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ActiveOffenceTransformer
import java.time.LocalDate

class ActiveOffenceTransformerTest {
  @Test
  fun transformToApi() {
    val result = ActiveOffenceTransformer().transformToApi(
      CaseDetailOffenceFactory()
        .withEventNumber("25")
        .withDescription("the description")
        .withDate(LocalDate.of(2022, 6, 5))
        .produce(),
    )

    assertThat(result.deliusEventNumber).isEqualTo("25")
    assertThat(result.offenceDescription).isEqualTo("the description")
    assertThat(result.offenceDate).isEqualTo(LocalDate.of(2022, 6, 5))
    assertThat(result.offenceId).isNull()
    assertThat(result.convictionId).isNull()
  }
}
