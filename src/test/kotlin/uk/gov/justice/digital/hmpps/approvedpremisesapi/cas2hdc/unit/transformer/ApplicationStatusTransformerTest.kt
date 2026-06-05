package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference.Cas2HdcPersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference.Cas2HdcPersistedApplicationStatusDetail
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
        val internalModel = Cas2HdcPersistedApplicationStatus(
          id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
          name = "cancelled",
          label = "Referral cancelled",
          description = "The application has been cancelled.",
          statusDetails = listOf(
            Cas2HdcPersistedApplicationStatusDetail(
              id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
              name = "changeOfCircumstances",
              label = "Change of circumstances",
            ),
          ),
          isActive = true,
        )

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2HdcApplicationStatus(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "cancelled",
            label = "Referral cancelled",
            statusDetails = listOf(
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
                name = "changeOfCircumstances",
                label = "Change of circumstances",
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
        val internalModel = Cas2HdcPersistedApplicationStatus(
          id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
          name = "cancelled",
          label = "Referral cancelled",
          description = "The application has been cancelled.",
          isActive = true,
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
