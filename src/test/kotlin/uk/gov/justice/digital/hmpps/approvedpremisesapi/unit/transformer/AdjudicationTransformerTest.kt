package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
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
import java.time.ZoneOffset
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
              .produce(),
          ),
        ).produce(),
        getLast12MonthsOnly = false,
      )
    }

    assertThat(exception.message).isEqualTo("Agency UNKNOWN not found")
  }

  @Test
  fun `transformToApi transforms charge with held hearing correctly`() {
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
              findingCode = "QUASHED",
            ),
          ),
        ),
      ),
      agencies = listOf(
        Agency(
          agencyId = "PLACE",
          description = "THE PLACE",
          agencyType = "INST",
        ),
      ),
    )

    val transformed = adjudicationTransformer.transformToApi(adjudicationsPage, false)
    assertThat(transformed).isEqualTo(
      listOf(
        ApiAdjudication(
          id = 12345,
          reportedAt = Instant.parse("2022-10-28T15:15:15Z"),
          establishment = "THE PLACE",
          offenceDescription = "Something, something",
          hearingHeld = true,
          finding = "QUASHED",
        ),
      ),
    )
  }

  @Test
  fun `transformToApi transforms charge without held hearing correctly`() {
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
              findingCode = null,
            ),
          ),
        ),
      ),
      agencies = listOf(
        Agency(
          agencyId = "PLACE",
          description = "THE PLACE",
          agencyType = "INST",
        ),
      ),
    )

    val transformed = adjudicationTransformer.transformToApi(adjudicationsPage, false)
    assertThat(transformed).isEqualTo(
      listOf(
        ApiAdjudication(
          id = 12345,
          reportedAt = Instant.parse("2022-10-28T15:15:15Z"),
          establishment = "THE PLACE",
          offenceDescription = "Something, something",
          hearingHeld = false,
          finding = null,
        ),
      ),
    )
  }

  @Nested
  inner class GetLast12MonthsOnly {

    val recentAdjudicationReportDate: LocalDateTime = LocalDateTime.now().minusMinutes(5)
    val justInsideAdjudicationReportDate: LocalDateTime = LocalDateTime.now().minusMonths(12).plusDays(1)
    val justOutsideAdjudicationReportDate: LocalDateTime = LocalDateTime.now().minusMonths(12)
    lateinit var adjudicationsPage: AdjudicationsPage

    fun setupAdjudications() {
      adjudicationsPage = AdjudicationsPage(
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
                findingCode = "QUASHED",
              ),
            ),
          ),
          Adjudication(
            adjudicationNumber = 23456,
            reportTime = recentAdjudicationReportDate,
            agencyIncidentId = 89101,
            agencyId = "PLACE2",
            partySeq = 1,
            adjudicationCharges = listOf(
              AdjudicationCharge(
                oicChargeId = "CHARGE",
                offenceCode = "OFFENCE",
                offenceDescription = "Very recent, very recent",
                findingCode = "QUASHED",
              ),
            ),
          ),
          Adjudication(
            adjudicationNumber = 34567,
            reportTime = justInsideAdjudicationReportDate,
            agencyIncidentId = 91012,
            agencyId = "PLACE",
            partySeq = 1,
            adjudicationCharges = listOf(
              AdjudicationCharge(
                oicChargeId = "CHARGE",
                offenceCode = "OFFENCE",
                offenceDescription = "Just inside, just inside",
                findingCode = "QUASHED",
              ),
            ),
          ),
          Adjudication(
            adjudicationNumber = 45678,
            reportTime = justOutsideAdjudicationReportDate,
            agencyIncidentId = 10112,
            agencyId = "PLACE2",
            partySeq = 1,
            adjudicationCharges = listOf(
              AdjudicationCharge(
                oicChargeId = "CHARGE",
                offenceCode = "OFFENCE",
                offenceDescription = "Just outside, just outside",
                findingCode = "QUASHED",
              ),
            ),
          ),
        ),
        agencies = listOf(
          Agency(
            agencyId = "PLACE",
            description = "THE PLACE",
            agencyType = "INST",
          ),
          Agency(
            agencyId = "PLACE2",
            description = "THE PLACE 2",
            agencyType = "INST",
          ),
        ),
      )
    }

    @Test
    fun `transformToApi returns all charges when getLast12MonthsOnly is false`() {
      setupAdjudications()
      val transformed = adjudicationTransformer.transformToApi(adjudicationsPage, false)
      assertThat(transformed.size).isEqualTo(4)
      assertThat(transformed).isEqualTo(
        listOf(
          ApiAdjudication(
            id = 12345,
            reportedAt = Instant.parse("2022-10-28T15:15:15Z"),
            establishment = "THE PLACE",
            offenceDescription = "Something, something",
            hearingHeld = true,
            finding = "QUASHED",
          ),
          ApiAdjudication(
            id = 23456,
            reportedAt = recentAdjudicationReportDate.toInstant(ZoneOffset.UTC),
            establishment = "THE PLACE 2",
            offenceDescription = "Very recent, very recent",
            hearingHeld = true,
            finding = "QUASHED",
          ),
          ApiAdjudication(
            id = 34567,
            reportedAt = justInsideAdjudicationReportDate.toInstant(ZoneOffset.UTC),
            establishment = "THE PLACE",
            offenceDescription = "Just inside, just inside",
            hearingHeld = true,
            finding = "QUASHED",
          ),
          ApiAdjudication(
            id = 45678,
            reportedAt = justOutsideAdjudicationReportDate.toInstant(ZoneOffset.UTC),
            establishment = "THE PLACE 2",
            offenceDescription = "Just outside, just outside",
            hearingHeld = true,
            finding = "QUASHED",
          ),
        ),
      )
    }

    @Test
    fun `transformToApi excludes charges older than 12 months when getLast12MonthsOnly is true`() {
      setupAdjudications()

      val transformed = adjudicationTransformer.transformToApi(adjudicationsPage, true)
      assertThat(transformed.size).isEqualTo(2)
      assertThat(transformed).isEqualTo(
        listOf(
          ApiAdjudication(
            id = 23456,
            reportedAt = recentAdjudicationReportDate.toInstant(ZoneOffset.UTC),
            establishment = "THE PLACE 2",
            offenceDescription = "Very recent, very recent",
            hearingHeld = true,
            finding = "QUASHED",
          ),
          ApiAdjudication(
            id = 34567,
            reportedAt = justInsideAdjudicationReportDate.toInstant(ZoneOffset.UTC),
            establishment = "THE PLACE",
            offenceDescription = "Just inside, just inside",
            hearingHeld = true,
            finding = "QUASHED",
          ),
        ),
      )
    }
  }
}
