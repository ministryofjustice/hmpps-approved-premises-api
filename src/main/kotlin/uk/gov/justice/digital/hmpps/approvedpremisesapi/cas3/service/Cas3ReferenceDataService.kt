package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.Cas3RefDataType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.toCas3ReferenceData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferenceData

@Service
class Cas3ReferenceDataService(
  private val cas3BedspaceCharacteristicRepository: Cas3BedspaceCharacteristicRepository,
  private val cas3PremisesCharacteristicRepository: Cas3PremisesCharacteristicRepository,
  private val cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository,
) {
  fun getReferenceData(type: Cas3RefDataType): List<Cas3ReferenceData> {
    val results = when (type) {
      Cas3RefDataType.BEDSPACE_CHARACTERISTICS -> cas3BedspaceCharacteristicRepository.findAllByActiveIsTrue()
      Cas3RefDataType.PREMISES_CHARACTERISTICS -> cas3PremisesCharacteristicRepository.findAllByActiveIsTrue()
      Cas3RefDataType.VOID_BEDSPACE_REASONS -> cas3VoidBedspaceReasonRepository.findAllActive()
    }
    return results.map { it.toCas3ReferenceData() }
  }
}
