package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer
import java.util.UUID

class ApplicationStatusTransformerTest {

  private val transformer = Cas2HdcApplicationStatusTransformer()

  @Nested
  inner class TransformModelToApi {

    @Nested
    inner class WhenThereAreStatusDetails {
      @Test
      fun `returns the expected properties from the internal _model_`() {
        val internalModel = Cas2PersistedApplicationStatus(
          status = Cas2AssessmentStatus.CANCELLED,
          statusDetails = listOf(Cas2AssessmentStatusDetail.CREATED_IN_ERROR),
        )

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2HdcApplicationStatus(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "cancelled",
            label = "Referral cancelled",
            statusDetails = listOf(
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("d1d96185-d92a-450b-b47f-bcce50356eed"),
                name = "createdInError",
                label = "Created in error",
              ),
            ),
            description = "The application has been cancelled.",
          ),
        )
      }
    }

    @Nested
    inner class WhenThereAreNotStatusDetails {
      @Test
      fun `returns the expected properties from the internal _model_`() {
        val internalModel = Cas2PersistedApplicationStatus(
          status = Cas2AssessmentStatus.CANCELLED,
        )

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2HdcApplicationStatus(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "cancelled",
            label = "Referral cancelled",
            description = "The application has been cancelled.",
            statusDetails = emptyList(),
          ),
        )
      }
    }
  }
}
