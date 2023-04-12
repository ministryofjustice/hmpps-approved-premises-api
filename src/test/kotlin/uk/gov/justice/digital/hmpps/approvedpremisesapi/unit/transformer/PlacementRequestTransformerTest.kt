package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer

class PlacementRequestTransformerTest {
  private val personTransformer = PersonTransformer()
  private val risksTransformer = RisksTransformer()

  private val placementRequestTransformer = PlacementRequestTransformer(personTransformer, risksTransformer)

  @Test
  fun `transformJpaToApi transforms a basic placement request entity`() {
    val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()
    val inmateDetail = InmateDetailFactory().produce()

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withReleaseType("licence")
      .withCreatedByUser(user)
      .produce()

    val assessment = AssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val placementRequestEntity = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withEssentialCriteria(
        listOf(
          CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
          CharacteristicEntityFactory().withPropertyName("isRecoveryFocussed").produce(),
          CharacteristicEntityFactory().withPropertyName("someOtherPropertyName").produce(),
        ),
      )
      .withDesirableCriteria(
        listOf(
          CharacteristicEntityFactory().withPropertyName("hasWideStepFreeAccess").produce(),
          CharacteristicEntityFactory().withPropertyName("hasLift").produce(),
          CharacteristicEntityFactory().withPropertyName("hasBrailleSignage").produce(),
          CharacteristicEntityFactory().withPropertyName("somethingElse").produce(),
        ),
      )
      .withAllocatedToUser(user)
      .produce()

    val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, offenderDetailSummary, inmateDetail)

    assertThat(result).isEqualTo(
      PlacementRequest(
        id = placementRequestEntity.id,
        gender = placementRequestEntity.gender,
        type = placementRequestEntity.apType,
        expectedArrival = placementRequestEntity.expectedArrival,
        duration = placementRequestEntity.duration,
        location = placementRequestEntity.postcodeDistrict.outcode,
        radius = placementRequestEntity.radius,
        essentialCriteria = listOf(PlacementCriteria.isSemiSpecialistMentalHealth, PlacementCriteria.isRecoveryFocussed),
        desirableCriteria = listOf(PlacementCriteria.hasWideStepFreeAccess, PlacementCriteria.hasLift, PlacementCriteria.hasBrailleSignage),
        mentalHealthSupport = placementRequestEntity.mentalHealthSupport,
        person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
        risks = risksTransformer.transformDomainToApi(placementRequestEntity.application.riskRatings!!, placementRequestEntity.application.crn),
        applicationId = application.id,
        assessmentId = assessment.id,
        releaseType = ReleaseTypeOption.licence,
      ),
    )
  }
}
