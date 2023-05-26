package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConvictionFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConvictionTransformer
import java.time.LocalDate
import java.time.LocalDateTime

class ConvictionTransformerTest {
  private val convictionTransformer = ConvictionTransformer()

  @Test
  fun `transformToApi maps each Offence correctly`() {
    val conviction = ConvictionFactory()
      .withConvictionId(12345)
      .withIndex("5")
      .withOffences(
        listOf(
          OffenceFactory()
            .withOffenceId("1")
            .withMainCategoryDescription("Main Category 1")
            .withSubCategoryDescription("Sub Category 1")
            .withOffenceDate(LocalDateTime.parse("2022-12-06T00:00:00"))
            .produce(),
          OffenceFactory()
            .withOffenceId("2")
            .withMainCategoryDescription("Main Category 2")
            .withSubCategoryDescription("Sub Category 2")
            .withOffenceDate(LocalDateTime.parse("2022-12-05T00:00:00"))
            .produce(),
        ),
      )
      .produce()

    assertThat(convictionTransformer.transformToApi(conviction)).containsExactlyInAnyOrder(
      ActiveOffence(
        deliusEventNumber = "5",
        offenceDescription = "Main Category 1 - Sub Category 1",
        offenceId = "1",
        convictionId = 12345,
        offenceDate = LocalDate.parse("2022-12-06"),
      ),
      ActiveOffence(
        deliusEventNumber = "5",
        offenceDescription = "Main Category 2 - Sub Category 2",
        offenceId = "2",
        convictionId = 12345,
        offenceDate = LocalDate.parse("2022-12-05"),
      ),
    )
  }

  @Test
  fun `transformToApi omits sub category description where it is the same as main category description`() {
    val conviction = ConvictionFactory()
      .withConvictionId(12345)
      .withIndex("5")
      .withOffences(
        listOf(
          OffenceFactory()
            .withOffenceId("1")
            .withMainCategoryDescription("A Description")
            .withSubCategoryDescription("A Description")
            .withOffenceDate(LocalDateTime.parse("2022-12-06T00:00:00"))
            .produce(),
        ),
      )
      .produce()

    assertThat(convictionTransformer.transformToApi(conviction)).containsExactlyInAnyOrder(
      ActiveOffence(
        deliusEventNumber = "5",
        offenceDescription = "A Description",
        offenceId = "1",
        convictionId = 12345,
        offenceDate = LocalDate.parse("2022-12-06"),
      ),
    )
  }
}
