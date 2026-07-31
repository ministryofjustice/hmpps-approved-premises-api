package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity

fun IntegrationTestBase.givenAPlacementRequirements(
  desirableCharacteristics: List<Cas1CharacteristicEntity>,
  essentialCharacteristics: List<Cas1CharacteristicEntity>,
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
