package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

fun IntegrationTestBase.givenACas3Premises(
  probationRegion: ProbationRegionEntity,
  status: Cas3PremisesStatus = randomOf(Cas3PremisesStatus.entries),
  startDate: LocalDate? = null,
  endDate: LocalDate? = null,
) = givenACas3Premises(
  probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegion)
  },
  status = status,
  startDate = startDate,
  endDate = endDate,
)

fun IntegrationTestBase.givenACas3Premises(
  probationDeliveryUnit: ProbationDeliveryUnitEntity = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegionEntityFactory.produceAndPersist())
  },
  status: Cas3PremisesStatus = randomOf(Cas3PremisesStatus.entries),
  startDate: LocalDate? = null,
  endDate: LocalDate? = null,
) = givenACas3Premises(
  name = randomStringMultiCaseWithNumbers(8),
  probationDeliveryUnit = probationDeliveryUnit,
  status = status,
  startDate = startDate,
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
  startDate: LocalDate? = null,
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
    withStartDate(startDate ?: LocalDate.now().minusDays(180))
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

fun IntegrationTestBase.givenACas3PremisesWithUser(
  roles: List<UserRole> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  premisesStatus: Cas3PremisesStatus = Cas3PremisesStatus.online,
  premisesStartDate: LocalDate = LocalDate.now().minusDays(180),
  premisesEndDate: LocalDate? = null,
  block: (user: UserEntity, jwt: String, premises: Cas3PremisesEntity) -> Unit,
) {
  givenAUser(
    roles = roles,
    probationRegion = probationRegion,
  ) { user, jwt ->
    val premises = givenACas3Premises(
      region = user.probationRegion,
      status = premisesStatus,
      startDate = premisesStartDate,
      endDate = premisesEndDate,
    )
    block(user, jwt, premises)
  }
}

fun IntegrationTestBase.givenACas3PremisesWithBedspaces(
  region: ProbationRegionEntity = givenAProbationRegion(),
  bedspaceCount: Int = 1,
  bedspaceReferences: List<String> = emptyList(),
  bedspaceCharacteristics: List<Cas3BedspaceCharacteristicEntity> = emptyList(),
  bedspacesStartDates: List<LocalDate> = emptyList(),
  bedspacesEndDates: List<LocalDate?> = emptyList(),
  bedspacesCreatedDate: List<LocalDate> = emptyList(),
  block: (premises: Cas3PremisesEntity, bedspaces: List<Cas3BedspacesEntity>) -> Unit,
) {
  val premises = givenACas3Premises(region, startDate = bedspacesStartDates[0])
  val bedspaces = mutableListOf<Cas3BedspacesEntity>()

  repeat(bedspaceCount) { index ->
    val bedspaceReference = if (bedspaceReferences.size > index) bedspaceReferences[index] else randomStringMultiCaseWithNumbers(8)
    val startDate = if (bedspacesStartDates.size > index) bedspacesStartDates[index] else LocalDate.now().minusDays(30)
    val endDate = if (bedspacesEndDates.size > index) bedspacesEndDates[index] else null
    val createdDate = if (bedspacesCreatedDate.size > index) bedspacesCreatedDate[index] else LocalDate.now().minusDays(90)
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withReference(bedspaceReference)
      withStartDate(startDate)
      withEndDate(endDate)
      withCreatedDate(createdDate)
      withCharacteristics(bedspaceCharacteristics.toMutableList())
    }
    bedspaces.add(bedspace)
  }

  block(premises, bedspaces)
}

fun IntegrationTestBase.givenACas3PremisesComplete(
  roles: List<UserRole> = emptyList(),
  bedspaceCount: Int = 1,
  premisesStatus: Cas3PremisesStatus = Cas3PremisesStatus.online,
  premisesStartDate: LocalDate = LocalDate.now().minusDays(180),
  premisesEndDate: LocalDate? = null,
  bedspaceStartDates: List<LocalDate> = emptyList(),
  bedspaceEndDates: List<LocalDate?> = emptyList(),
  bedspaceReferences: List<String> = emptyList(),
  bedspaceCharacteristics: List<Cas3BedspaceCharacteristicEntity> = emptyList(),
  block: (user: UserEntity, jwt: String, premises: Cas3PremisesEntity, bedspaces: List<Cas3BedspacesEntity>) -> Unit,
) {
  givenAUser(
    roles = roles,
  ) { user, jwt ->
    givenACas3PremisesWithBedspaces(
      region = user.probationRegion,
      bedspaceCount = bedspaceCount,
      bedspacesStartDates = bedspaceStartDates,
      bedspacesEndDates = bedspaceEndDates,
      bedspaceReferences = bedspaceReferences,
      bedspaceCharacteristics = bedspaceCharacteristics,
    ) { premises, bedspaces ->
      // Update premises with status and end date if provided
      if (premisesStatus != premises.status || premisesEndDate != premises.endDate || premisesStartDate != premises.startDate) {
        premises.status = premisesStatus
        premises.startDate = premisesStartDate
        premises.endDate = premisesEndDate
        cas3PremisesRepository.save(premises)
      }
      block(user, jwt, premises, bedspaces)
    }
  }
}
