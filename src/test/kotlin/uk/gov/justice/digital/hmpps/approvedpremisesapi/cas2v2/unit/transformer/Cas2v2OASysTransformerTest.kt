package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2OASysTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RisksToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.apandoasys.OASysAssessmentSummaryFactory
import java.time.Instant
import java.time.OffsetDateTime

class Cas2v2OASysTransformerTest {

  private val transformer = Cas2v2OASysTransformer()

  @Nested
  inner class ToOASysMetadataDto {

    @Test
    fun found() {
      val result = transformer.toOASysAssessmentMetadataDto(
        OASysAssessmentSummaryFactory()
          .withInitiationDate(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"))
          .withCompletedDate(OffsetDateTime.parse("2008-12-03T10:15:30+01:00"))
          .produce(),
      )

      assertThat(result.hasApplicableAssessment).isTrue()
      assertThat(result.dateStarted).isEqualTo(Instant.parse("2007-12-03T10:15:30+01:00"))
      assertThat(result.dateCompleted).isEqualTo(Instant.parse("2008-12-03T10:15:30+01:00"))
    }

    @Test
    fun not_found() {
      val result = transformer.toOASysAssessmentMetadataDto(null)

      assertThat(result.hasApplicableAssessment).isFalse()
      assertThat(result.dateStarted).isNull()
      assertThat(result.dateCompleted).isNull()
    }
  }

  @Nested
  inner class ToCas2v2OAsysRiskToSelfDto {

    @Test
    fun found() {
      val result = transformer.toOASysRiskToSelfDto(
        RisksToTheIndividualFactory()
          .withAnalysisSuicideSelfharm("self harm answer")
          .withAnalysisVulnerabilities("vulnerability answer")
          .withInitiationDate(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"))
          .withDateCompleted(OffsetDateTime.parse("2008-12-03T10:15:30+01:00"))
          .produce(),
      )

      assertThat(result.metadata.hasApplicableAssessment).isTrue()
      assertThat(result.metadata.dateStarted).isEqualTo(Instant.parse("2007-12-03T10:15:30+01:00"))
      assertThat(result.metadata.dateCompleted).isEqualTo(Instant.parse("2008-12-03T10:15:30+01:00"))

      assertThat(result.analysisSuicideSelfharm).isEqualTo("self harm answer")
      assertThat(result.analysisVulnerabilities).isEqualTo("vulnerability answer")
    }

    @Test
    fun not_found() {
      val result = transformer.toOASysRiskToSelfDto(null)

      assertThat(result.metadata.hasApplicableAssessment).isFalse()
      assertThat(result.metadata.dateStarted).isNull()
      assertThat(result.metadata.dateCompleted).isNull()

      assertThat(result.analysisSuicideSelfharm).isNull()
      assertThat(result.analysisVulnerabilities).isNull()
    }
  }
}
