package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailOffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConvictionFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import java.time.LocalDate
import java.time.LocalDateTime

class OffenceTransformerTest {
  private val offenceTransformer = OffenceTransformer()

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

    assertThat(offenceTransformer.transformToApi(conviction)).containsExactlyInAnyOrder(
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
            .withOffenceId("M1")
            .withMainCategoryDescription("A Description")
            .withSubCategoryDescription("A Description")
            .withOffenceDate(LocalDateTime.parse("2022-12-06T00:00:00"))
            .produce(),
        ),
      )
      .produce()

    assertThat(offenceTransformer.transformToApi(conviction)).containsExactlyInAnyOrder(
      ActiveOffence(
        deliusEventNumber = "5",
        offenceDescription = "A Description",
        offenceId = "M1",
        convictionId = 12345,
        offenceDate = LocalDate.parse("2022-12-06"),
      ),
    )
  }

  @Test
  fun `transformToApi from case-detail maps each Offence correctly`() {
    val caseDetail = CaseDetailFactory()
      .withOffences(
        listOf(
          CaseDetailOffenceFactory()
            .withId("M10")
            .withDescription("A first offence")
            .withDate(LocalDate.parse("2024-11-01"))
            .withEventNumber("20")
            .withEventId(30)
            .produce(),
          CaseDetailOffenceFactory()
            .withId("A100")
            .withDescription("A second offence")
            .withDate(LocalDate.parse("2024-11-11"))
            .withEventNumber("200")
            .withEventId(300)
            .produce(),
        ),
      )
      .produce()

    assertThat(offenceTransformer.transformToApi(caseDetail)).containsExactlyInAnyOrder(
      ActiveOffence(
        deliusEventNumber = "20",
        offenceDescription = "A first offence",
        offenceId = "M10",
        convictionId = 30,
        offenceDate = LocalDate.parse("2024-11-01"),
      ),
      ActiveOffence(
        deliusEventNumber = "200",
        offenceDescription = "A second offence",
        offenceId = "A100",
        convictionId = 300,
        offenceDate = LocalDate.parse("2024-11-11"),
      ),
    )
  }
}
