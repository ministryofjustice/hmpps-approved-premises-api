package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3OASysAssessmentInfoTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import java.time.Instant
import java.time.OffsetDateTime

class Cas3OASysAssessmentInfoTransformerTest {

  private val transformer = Cas3OASysAssessmentInfoTransformer()

  @Nested
  inner class ToAssessmentMetadata {

    @Test
    fun `has applicable assessment`() {
      val initiationDate = OffsetDateTime.parse("2020-05-02T12:01:00+00:00")
      val completionDate = OffsetDateTime.parse("2021-05-02T12:02:00+00:00")
      val lastUpdatedDate = OffsetDateTime.parse("2021-06-03T09:04:00+00:00")

      val result = transformer.toAssessmentMetadata(
        OffenceDetailsFactory()
          .withInitiationDate(initiationDate)
          .withDateCompleted(completionDate)
          .withLastUpdatedDate(lastUpdatedDate)
          .produce(),
      )

      assertThat(result.hasApplicableAssessment).isTrue()
      assertThat(result.dateStarted).isEqualTo(Instant.parse("2020-05-02T12:01:00+00:00"))
      assertThat(result.dateCompleted).isEqualTo(Instant.parse("2021-05-02T12:02:00+00:00"))
      assertThat(result.lastUpdatedDate).isEqualTo(Instant.parse("2021-06-03T09:04:00Z"))
    }

    @Test
    fun `has applicable assessment with no last updated date`() {
      val initiationDate = OffsetDateTime.parse("2020-05-02T12:01:00+00:00")
      val completionDate = OffsetDateTime.parse("2021-05-02T12:02:00+00:00")

      val result = transformer.toAssessmentMetadata(
        OffenceDetailsFactory()
          .withInitiationDate(initiationDate)
          .withDateCompleted(completionDate)
          .withLastUpdatedDate(null)
          .produce(),
      )

      assertThat(result.hasApplicableAssessment).isTrue()
      assertThat(result.dateStarted).isEqualTo(Instant.parse("2020-05-02T12:01:00+00:00"))
      assertThat(result.dateCompleted).isEqualTo(Instant.parse("2021-05-02T12:02:00+00:00"))
      assertThat(result.lastUpdatedDate).isNull()
    }

    @Test
    fun `no applicable assessment`() {
      val result = transformer.toAssessmentMetadata(null)

      assertThat(result.hasApplicableAssessment).isFalse()
      assertThat(result.dateStarted).isNull()
      assertThat(result.dateCompleted).isNull()
      assertThat(result.lastUpdatedDate).isNull()
    }
  }
}
