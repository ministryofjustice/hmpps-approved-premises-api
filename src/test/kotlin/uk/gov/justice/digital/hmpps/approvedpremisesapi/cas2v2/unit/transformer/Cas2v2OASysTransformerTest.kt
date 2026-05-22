package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2OASysTransformer
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

    fun not_found() {
      val result = transformer.toOASysAssessmentMetadataDto(null)

      assertThat(result.hasApplicableAssessment).isFalse()
      assertThat(result.dateStarted).isNull()
      assertThat(result.dateCompleted).isNull()
    }
  }
}
