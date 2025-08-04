package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Premises
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
  fun transformDomainToApi(premisesEntity: TemporaryAccommodationPremisesEntity) = Cas3Premises(
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
    startDate = premisesEntity.startDate,
    status = premisesEntity.status.transformStatus(),
    notes = premisesEntity.notes,
    turnaroundWorkingDays = premisesEntity.turnaroundWorkingDays,
    totalOnlineBedspaces = premisesEntity.getTotalOnlineBedspaces(),
    totalUpcomingBedspaces = premisesEntity.getTotalUpcomingBedspaces(),
    totalArchivedBedspaces = premisesEntity.getTotalArchivedBedspaces(),
  )

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun PropertyStatus.transformStatus() = when (this) {
    PropertyStatus.active -> Cas3PremisesStatus.online
    PropertyStatus.archived -> Cas3PremisesStatus.archived
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
