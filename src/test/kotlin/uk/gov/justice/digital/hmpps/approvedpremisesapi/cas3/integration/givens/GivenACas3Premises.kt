package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

fun IntegrationTestBase.givenACas3Premises(probationRegion: ProbationRegionEntity) = givenACas3Premises(
  probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegion)
  },
)

fun IntegrationTestBase.givenACas3Premises(
  name: String = randomStringMultiCaseWithNumbers(8),
  probationDeliveryUnit: ProbationDeliveryUnitEntity = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegionEntityFactory.produceAndPersist())
  },
  localAuthorityArea: LocalAuthorityAreaEntity = localAuthorityEntityFactory.produceAndPersist(),
  status: PropertyStatus = randomOf(PropertyStatus.entries),
  postCode: String = randomPostCode(),
  characteristics: List<Cas3PremisesCharacteristicEntity> = emptyList(),
  id: UUID = UUID.randomUUID(),
): Cas3PremisesEntity = cas3PremisesEntityFactory
  .produceAndPersist {
    withId(id)
    withName(name)
    withProbationDeliveryUnit(probationDeliveryUnit)
    withLocalAuthorityArea(localAuthorityArea)
    withStatus(status)
    withPostcode(postCode)
    withCharacteristics(characteristics.toMutableList())
  }
