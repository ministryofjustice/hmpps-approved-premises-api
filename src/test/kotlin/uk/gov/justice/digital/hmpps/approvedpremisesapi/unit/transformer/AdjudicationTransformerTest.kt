package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationChargeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationCharge
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer
import java.time.Instant
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication as ApiAdjudication

class AdjudicationTransformerTest {
  private val adjudicationTransformer = AdjudicationTransformer()

  @Test
  fun `transformToApi throws when agency in results not in agency lookup`() {
    val exception = assertThrows<RuntimeException> {
      adjudicationTransformer.transformToApi(
        AdjudicationsPageFactory().withResults(
          listOf(
            AdjudicationFactory()
              .withAgencyId("UNKNOWN")
              .withCharges(listOf(AdjudicationChargeFactory().produce()))
              .produce()
          )
        ).produce()
      )
    }

    assertThat(exception.message).isEqualTo("Agency UNKNOWN not found")
  }

  @Test
  fun `transformToApi transforms chage with held hearing correctly`() {
    val adjudicationsPage = AdjudicationsPage(
      results = listOf(
        Adjudication(
          adjudicationNumber = 12345,
          reportTime = LocalDateTime.parse("2022-10-28T15:15:15"),
          agencyIncidentId = 78910,
          agencyId = "PLACE",
          partySeq = 1,
          adjudicationCharges = listOf(
            AdjudicationCharge(
              oicChargeId = "CHARGE",
              offenceCode = "OFFENCE",
              offenceDescription = "Something, something",
              findingCode = "QUASHED"
            )
          )
        )
      ),
      agencies = listOf(
        Agency(
          agencyId = "PLACE",
          description = "THE PLACE",
          agencyType = "INST"
        )
      )
    )

    val transformed = adjudicationTransformer.transformToApi(adjudicationsPage)
    assertThat(transformed).isEqualTo(
      listOf(
        ApiAdjudication(
          id = 12345,
          reportedAt = Instant.parse("2022-10-28T15:15:15Z"),
          establishment = "THE PLACE",
          offenceDescription = "Something, something",
          hearingHeld = true,
          finding = "QUASHED"
        )
      )
    )
  }

  @Test
  fun `transformToApi transforms chage without held hearing correctly`() {
    val adjudicationsPage = AdjudicationsPage(
      results = listOf(
        Adjudication(
          adjudicationNumber = 12345,
          reportTime = LocalDateTime.parse("2022-10-28T15:15:15"),
          agencyIncidentId = 78910,
          agencyId = "PLACE",
          partySeq = 1,
          adjudicationCharges = listOf(
            AdjudicationCharge(
              oicChargeId = "CHARGE",
              offenceCode = "OFFENCE",
              offenceDescription = "Something, something",
              findingCode = null
            )
          )
        )
      ),
      agencies = listOf(
        Agency(
          agencyId = "PLACE",
          description = "THE PLACE",
          agencyType = "INST"
        )
      )
    )

    val transformed = adjudicationTransformer.transformToApi(adjudicationsPage)
    assertThat(transformed).isEqualTo(
      listOf(
        ApiAdjudication(
          id = 12345,
          reportedAt = Instant.parse("2022-10-28T15:15:15Z"),
          establishment = "THE PLACE",
          offenceDescription = "Something, something",
          hearingHeld = false,
          finding = null
        )
      )
    )
  }
}
