package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.DeliusContextOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import java.time.LocalDate

class OffenceTransformerTest {

  @Test
  fun transformToApi() {
    val result = OffenceTransformer().transformToApi(
      DeliusContextOffence(
        description = "theDescription",
        date = LocalDate.of(2025, 1, 2),
        main = true,
        eventNumber = "theEventNumber",
        code = "theCode",
      ),
    )

    assertThat(result.description).isEqualTo("theDescription")
    assertThat(result.offenceDate).isEqualTo(LocalDate.of(2025, 1, 2))
    assertThat(result.main).isEqualTo(true)
    assertThat(result.deliusEventNumber).isEqualTo("theEventNumber")
    assertThat(result.code).isEqualTo("theCode")
  }
}
