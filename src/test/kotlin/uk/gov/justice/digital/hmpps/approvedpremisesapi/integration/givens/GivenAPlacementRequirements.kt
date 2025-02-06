package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity

fun IntegrationTestBase.givenAPlacementRequirements(
  desirableCharacteristics: List<CharacteristicEntity>,
  essentialCharacteristics: List<CharacteristicEntity>,
): PlacementRequirementsEntity {
  val (user) = givenAUser()

  return placementRequirementsFactory.produceAndPersist {
    withAssessment(
      givenAnAssessmentForApprovedPremises(
        allocatedToUser = null,
        createdByUser = user,
      ).first,
    )
    withApplication(
      givenAnApplication(
        createdByUser = user,
      ),
    )
    withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
    withDesirableCriteria(desirableCharacteristics)
    withEssentialCriteria(essentialCharacteristics)
  }
}
