package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

fun IntegrationTestBase.givenACas3Premises(
  probationRegion: ProbationRegionEntity,
  status: Cas3PremisesStatus = randomOf(Cas3PremisesStatus.entries),
  endDate: LocalDate? = null,
) = givenACas3Premises(
  probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegion)
  },
  status = status,
  endDate = endDate,
)

fun IntegrationTestBase.givenACas3Premises(
  probationDeliveryUnit: ProbationDeliveryUnitEntity = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegionEntityFactory.produceAndPersist())
  },
  status: Cas3PremisesStatus = randomOf(Cas3PremisesStatus.entries),
  endDate: LocalDate? = null,
) = givenACas3Premises(
  name = randomStringMultiCaseWithNumbers(8),
  probationDeliveryUnit = probationDeliveryUnit,
  status = status,
  endDate = endDate,
)

fun IntegrationTestBase.givenACas3Premises(
  name: String = randomStringMultiCaseWithNumbers(8),
  probationDeliveryUnit: ProbationDeliveryUnitEntity = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegionEntityFactory.produceAndPersist())
  },
  localAuthorityArea: LocalAuthorityAreaEntity = localAuthorityEntityFactory.produceAndPersist(),
  status: Cas3PremisesStatus = randomOf(Cas3PremisesStatus.entries),
  postCode: String = randomPostCode(),
  characteristics: List<Cas3PremisesCharacteristicEntity> = emptyList(),
  id: UUID = UUID.randomUUID(),
  startDate: LocalDate = LocalDate.now().minusDays(180),
  endDate: LocalDate? = null,
): Cas3PremisesEntity = cas3PremisesEntityFactory
  .produceAndPersist {
    withId(id)
    withName(name)
    withProbationDeliveryUnit(probationDeliveryUnit)
    withLocalAuthorityArea(localAuthorityArea)
    withStatus(status)
    withPostcode(postCode)
    withCharacteristics(characteristics.toMutableList())
    withStartDate(startDate)
    withEndDate(endDate)
  }

fun IntegrationTestBase.givenACas3Premises(
  region: ProbationRegionEntity = givenAProbationRegion(),
  startDate: LocalDate,
  endDate: LocalDate? = null,
  status: Cas3PremisesStatus = Cas3PremisesStatus.online,
  block: ((premises: Cas3PremisesEntity) -> Unit)? = null,
): Cas3PremisesEntity {
  val premises = cas3PremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withProbationDeliveryUnit(
      probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(region)
      },
    )
    withStatus(status)
    withStartDate(startDate)
    withEndDate(endDate)
  }

  block?.invoke(premises)
  return premises
}

fun IntegrationTestBase.givenACas3PremisesWithBedspaces(
  region: ProbationRegionEntity = givenAProbationRegion(),
  bedspaceCount: Int = 1,
  bedspaceReferences: List<String> = emptyList(),
  bedspaceCharacteristics: List<Cas3BedspaceCharacteristicEntity> = emptyList(),
  bedspacesStartDates: List<LocalDate> = emptyList(),
  bedspacesEndDates: List<LocalDate?> = emptyList(),
  block: (premises: Cas3PremisesEntity, bedspaces: List<Cas3BedspacesEntity>) -> Unit,
) {
  val premises = givenACas3Premises(region, startDate = bedspacesStartDates[0])
  val bedspaces = mutableListOf<Cas3BedspacesEntity>()

  repeat(bedspaceCount) { index ->
    val bedspaceReference = if (bedspaceReferences.size > index) bedspaceReferences[index] else randomStringMultiCaseWithNumbers(8)
    val startDate = if (bedspacesStartDates.size > index) bedspacesStartDates[index] else LocalDate.now().minusDays(30)
    val endDate = if (bedspacesEndDates.size > index) bedspacesEndDates[index] else null
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withReference(bedspaceReference)
      withStartDate(startDate)
      withEndDate(endDate)
      withCharacteristics(bedspaceCharacteristics.toMutableList())
    }
    bedspaces.add(bedspace)
  }

  block(premises, bedspaces)
}
