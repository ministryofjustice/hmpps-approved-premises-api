package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
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
        val internalModel = Cas2AssessmentStatus.CANCELLED

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2HdcApplicationStatus(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "cancelled",
            label = "Referral cancelled",
            statusDetails = listOf(
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("ba46bbe0-8fb6-4539-cccc-5586e6bfe8b6"),
                name = "nacroAssessedAsHighRisk",
                label = "NACRO assessed as high risk",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("ba46bbe0-8fb6-4539-895d-5586e6bfe8b6"),
                name = "assessedAsHighRisk",
                label = "Assessed as high risk",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("522bb736-aeb6-480f-a51a-2bf3dcfcd482"),
                name = "notEligible",
                label = "Not eligible",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("ccf43af1-359b-4a14-8941-85eefa88f016"),
                name = "noRecourseToPublicFunds",
                label = "No recourse to public funds",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("c149a14d-ba06-420a-b844-5edfc02da6b1"),
                name = "noPropertyAvailable",
                label = "No property available",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("3fbdccc9-4858-4ae4-abb5-bd2b90d96d96"),
                name = "noFemalePropertyAvailable",
                label = "No female property available",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("bc539d6d-c353-49fa-847f-6967a148c527"),
                name = "noAdaptedPropertyAvailable",
                label = "No adapted property available",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("78636840-0155-45d4-971e-fe8d2d6c660c"),
                name = "noSuitablePropertyAvailable",
                label = "No suitable property available",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("7e8749c9-5254-4dae-90ed-590cf9f59847"),
                name = "incompleteReferral",
                label = "Incomplete referral",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("d1d96185-d92a-450b-b47f-bcce50356eed"),
                name = "createdInError",
                label = "Created in error",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("f38f55c0-fda6-44f8-a3b1-a7c0a990bc51"),
                name = "hdcNotEligible",
                label = "HDC not eligible",
              ),
              Cas2HdcApplicationStatusDetail(
                id = UUID.fromString("4f1033ab-2dea-47ce-8a86-7c47b3ccadd8"),
                name = "personTransferredToAnotherPrison",
                label = "Person transferred to another prison",
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
        val internalModel = Cas2AssessmentStatus.AWAITING_ARRIVAL

        val apiRepresentation = transformer.transformModelToApi(internalModel)

        Assertions.assertThat(apiRepresentation).isEqualTo(
          Cas2HdcApplicationStatus(
            id = UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565"),
            name = "awaitingArrival",
            label = "Awaiting arrival",
            description = "The accommodation is arranged for the agreed dates.",
            statusDetails = emptyList(),
          ),
        )
      }
    }
  }
}
