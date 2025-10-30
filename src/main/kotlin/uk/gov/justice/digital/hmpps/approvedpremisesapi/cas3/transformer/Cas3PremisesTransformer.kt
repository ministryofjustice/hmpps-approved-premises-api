package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import java.time.LocalDate

@Component
class Cas3PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
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
    status = getPremisesStatus(premises),
    notes = premises.notes,
    turnaroundWorkingDays = premises.turnaroundWorkingDays,
    totalOnlineBedspaces = premises.countOnlineBedspaces(),
    totalUpcomingBedspaces = premises.countUpcomingBedspaces(),
    totalArchivedBedspaces = premises.countArchivedBedspaces(),
    archiveHistory = archiveHistory,
  )

  private fun getPremisesStatus(premises: Cas3PremisesEntity) = if (premises.isPremisesArchived()) Cas3PremisesStatus.archived else Cas3PremisesStatus.online

  private fun isPremisesScheduleToUnarchive(premisesStartDate: LocalDate) = premisesStartDate.takeIf { it.isAfter(LocalDate.now()) }
}
