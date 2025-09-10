package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer

@Component
class Cas3PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
) {
  private fun PropertyStatus.toCas3PremisesStatus(): Cas3PremisesStatus = when (this) {
    PropertyStatus.archived -> Cas3PremisesStatus.archived
    PropertyStatus.active -> Cas3PremisesStatus.online
  }

  fun toCas3Premises(premises: Cas3PremisesEntity, archiveHistory: List<Cas3PremisesArchiveAction>? = emptyList()) = Cas3Premises(
    id = premises.id,
    reference = premises.name,
    addressLine1 = premises.addressLine1,
    addressLine2 = premises.addressLine2,
    postcode = premises.postcode,
    town = premises.town,
    localAuthorityArea = localAuthorityAreaTransformer.transformJpaToApi(premises.localAuthorityArea!!),
    probationRegion = probationRegionTransformer.transformJpaToApi(premises.probationDeliveryUnit.probationRegion),
    probationDeliveryUnit = probationDeliveryUnitTransformer.transformJpaToApi(premises.probationDeliveryUnit),
    status = premises.status.toCas3PremisesStatus(),
    totalOnlineBedspaces = premises.getTotalOnlineBedspaces(),
    totalUpcomingBedspaces = premises.getTotalUpcomingBedspaces(),
    totalArchivedBedspaces = premises.getTotalArchivedBedspaces(),
    characteristics = null,
    premisesCharacteristics = premises.characteristics.map { it ->
      PremisesCharacteristic(
        it.id,
        it.name,
        it.description,
      ) // TODO more work here
    },
    startDate = premises.startDate,
    endDate = premises.endDate,
    notes = premises.notes,
    turnaroundWorkingDays = premises.turnaroundWorkingDays,
    archiveHistory = archiveHistory,
  )

  @Deprecated("This will be removed as part of the Cas3v2 premsises work")
  fun transformDomainToApi(premisesEntity: TemporaryAccommodationPremisesEntity, archiveHistory: List<Cas3PremisesArchiveAction> = emptyList()) = Cas3Premises(
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
    startDate = premisesEntity.createdAt?.toLocalDate(),
    endDate = premisesEntity.endDate,
    status = getPremisesStatus(premisesEntity),
    notes = premisesEntity.notes,
    turnaroundWorkingDays = premisesEntity.turnaroundWorkingDays,
    totalOnlineBedspaces = premisesEntity.getTotalOnlineBedspaces(),
    totalUpcomingBedspaces = premisesEntity.getTotalUpcomingBedspaces(),
    totalArchivedBedspaces = premisesEntity.getTotalArchivedBedspaces(),
    archiveHistory = archiveHistory,
  )

  private fun getPremisesStatus(premises: TemporaryAccommodationPremisesEntity) = if (premises.isPremisesArchived()) {
    Cas3PremisesStatus.archived
  } else {
    Cas3PremisesStatus.online
  }

  private fun TemporaryAccommodationPremisesEntity.getTotalOnlineBedspaces() = this.rooms.sumOf { room ->
    room.beds.count { it.isCas3BedspaceOnline() }
  }

  private fun TemporaryAccommodationPremisesEntity.getTotalUpcomingBedspaces() = this.rooms.sumOf { room ->
    room.beds.count { it.isCas3BedspaceUpcoming() }
  }

  private fun TemporaryAccommodationPremisesEntity.getTotalArchivedBedspaces() = this.rooms.sumOf { room ->
    room.beds.count { it.isCas3BedspaceArchived() }
  }
}
