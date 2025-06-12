package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationStatusTransformer
import java.util.UUID

class ApplicationStatusTransformerTest {

  private val transformer = ApplicationStatusTransformer()

  @Nested
  inner class TransformModelToApi {

    @Nested
    inner class WhenThereAreStatusDetails {
      @Test
      fun `returns the expected properties from the internal _model_`() {
        val internalModel = Cas2PersistedApplicationStatus(
          id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
          name = "cancelled",
          label = "Referral cancelled",
          description = "The application has been cancelled.",
          statusDetails = listOf(
            Cas2PersistedApplicationStatusDetail(
              id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
              name = "changeOfCircumstances",
              label = "Change of circumstances",
            ),
          ),
          isActive = true,
        )

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2ApplicationStatus(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "cancelled",
            label = "Referral cancelled",
            statusDetails = listOf(
              Cas2ApplicationStatusDetail(
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
        val internalModel = Cas2PersistedApplicationStatus(
          id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
          name = "cancelled",
          label = "Referral cancelled",
          description = "The application has been cancelled.",
          isActive = true,
        )

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2ApplicationStatus(
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
