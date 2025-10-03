package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesTotalBedspacesByStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import java.time.LocalDate

@Component
class Cas3PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
) {

  fun toCas3Premises(
    premises: Cas3PremisesEntity,
    archiveHistory: List<Cas3PremisesArchiveAction>? = emptyList(),
  ) = Cas3Premises(
    id = premises.id,
    reference = premises.name,
    addressLine1 = premises.addressLine1,
    addressLine2 = premises.addressLine2,
    town = premises.town,
    postcode = premises.postcode,
    localAuthorityArea = premises.localAuthorityArea?.let { localAuthorityAreaTransformer.transformJpaToApi(it) },
    probationRegion = probationRegionTransformer.transformJpaToApi(premises.probationDeliveryUnit.probationRegion),
    probationDeliveryUnit = probationDeliveryUnitTransformer.transformJpaToApi(premises.probationDeliveryUnit),
    characteristics = null, // this field will be removed when switching to v2..
    premisesCharacteristics = premises.characteristics.map { it.toCas3PremisesCharacteristic() },
    startDate = premises.startDate,
    endDate = premises.endDate,
    scheduleUnarchiveDate = isPremisesScheduleToUnarchive(premises.startDate),
    status = premises.status,
    notes = premises.notes,
    turnaroundWorkingDays = premises.turnaroundWorkingDays,
    totalOnlineBedspaces = premises.countOnlineBedspaces(),
    totalUpcomingBedspaces = premises.countUpcomingBedspaces(),
    totalArchivedBedspaces = premises.countArchivedBedspaces(),
    archiveHistory = archiveHistory,
  )

  fun transformDomainToApi(
    premisesEntity: TemporaryAccommodationPremisesEntity,
    totalBedspacesByStatus: TemporaryAccommodationPremisesTotalBedspacesByStatus,
    archiveHistory: List<Cas3PremisesArchiveAction> = emptyList(),
  ) = Cas3Premises(
    id = premisesEntity.id,
    reference = premisesEntity.name,
    addressLine1 = premisesEntity.addressLine1,
    addressLine2 = premisesEntity.addressLine2,
    town = premisesEntity.town,
    postcode = premisesEntity.postcode,
    localAuthorityArea = premisesEntity.localAuthorityArea?.let { localAuthorityAreaTransformer.transformJpaToApi(it) },
    probationRegion = probationRegionTransformer.transformJpaToApi(premisesEntity.probationRegion),
    probationDeliveryUnit = premisesEntity.probationDeliveryUnit?.let { probationDeliveryUnitTransformer.transformJpaToApi(it) }!!,
    characteristics = premisesEntity.characteristics.map(characteristicTransformer::transformJpaToApi).sortedBy { it.id },
    startDate = premisesEntity.createdAt.toLocalDate(),
    endDate = premisesEntity.endDate,
    scheduleUnarchiveDate = isPremisesScheduleToUnarchive(premisesEntity.startDate),
    status = getPremisesStatus(premisesEntity),
    notes = premisesEntity.notes,
    turnaroundWorkingDays = premisesEntity.turnaroundWorkingDays,
    totalOnlineBedspaces = totalBedspacesByStatus.onlineBedspaces,
    totalUpcomingBedspaces = totalBedspacesByStatus.upcomingBedspaces,
    totalArchivedBedspaces = totalBedspacesByStatus.archivedBedspaces,
    archiveHistory = archiveHistory,
  )

  private fun getPremisesStatus(premises: TemporaryAccommodationPremisesEntity) = if (premises.isPremisesArchived()) Cas3PremisesStatus.archived else Cas3PremisesStatus.online

  private fun isPremisesScheduleToUnarchive(premisesStartDate: LocalDate) = premisesStartDate.takeIf { it.isAfter(LocalDate.now()) }
}
