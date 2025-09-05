package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import jakarta.transaction.Transactional
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

@Service
class Cas3v2PremisesService(
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val cas3PremisesCharacteristicRepository: Cas3PremisesCharacteristicRepository,
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
) {
  fun getPremises(premisesId: UUID): Cas3PremisesEntity? = cas3PremisesRepository.findByIdOrNull(premisesId)
  fun findBedspace(premisesId: UUID, bedspaceId: UUID): Cas3BedspacesEntity? = cas3BedspacesRepository.findCas3Bedspace(premisesId = premisesId, bedspaceId = bedspaceId)

  @Transactional
  fun createNewPremises(
    reference: String,
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    probationDeliveryUnitId: UUID,
    characteristicIds: List<UUID>,
    notes: String?,
    turnaroundWorkingDays: Int?,
  ): CasResult<Cas3PremisesEntity> = validatedCasResult {

    val localAuthorityArea = localAuthorityAreaId?.let { localAuthorityAreaRepository.findByIdOrNull(it) }
    val probationDeliveryUnit =
      probationDeliveryUnitRepository.findByIdAndProbationRegionId(probationDeliveryUnitId, probationRegionId)

    val validatedCharacteristics =
      cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(characteristicIds)

    characteristicIds
      .filterNot { it in validatedCharacteristics.map { characteristic -> characteristic.id } }
      .forEach { id ->
        validationErrors["$.characteristics[$id]"] = "doesNotExist"
      }

    if (validatePremises(
        reference,
        addressLine1,
        postcode,
        probationRegionId,
        probationDeliveryUnit,
        turnaroundWorkingDays,
        localAuthorityArea,
      ).any()
    ) {
      return fieldValidationError
    }

    val premises = Cas3PremisesEntity(
      id = UUID.randomUUID(),
      name = reference,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      town = town,
      postcode = postcode,
      localAuthorityArea = localAuthorityArea,
      notes = notes.orEmpty(),
      status = PropertyStatus.active,
      probationDeliveryUnit = probationDeliveryUnit!!,
      characteristics = validatedCharacteristics.toMutableList(),
      turnaroundWorkingDays = turnaroundWorkingDays ?: 2,
      bookings = mutableListOf(),
      bedspaces = mutableListOf(),
      startDate = LocalDate.now(),
      endDate = null,
    )

    cas3PremisesRepository.save(premises)
    return success(premises)
  }

  private fun CasResultValidatedScope<Cas3PremisesEntity>.validatePremises(
    reference: String,
    addressLine1: String,
    postcode: String,
    probationRegionId: UUID,
    probationDeliveryUnit: ProbationDeliveryUnitEntity?,
    turnaroundWorkingDays: Int?,
    localAuthorityArea: LocalAuthorityAreaEntity?,
  ): ValidationErrors {

    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)
    if (probationRegion == null) {
      "$.probationRegionId" hasValidationError "doesNotExist"
    }

    if (localAuthorityArea == null) {
      "$.localAuthorityAreaId" hasValidationError "doesNotExist"
    }

    if (probationDeliveryUnit == null) {
      "$.probationDeliveryUnitId" hasValidationError "doesNotExist"
    }

    if (reference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    } else if (cas3PremisesRepository.countByNameIgnoreCase(reference) > 0) {
      "$.reference" hasValidationError "notUnique"
    }

    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    if (turnaroundWorkingDays != null && turnaroundWorkingDays < 0) {
      "$.turnaroundWorkingDays" hasValidationError "isNotAPositiveInteger"
    }

    return validationErrors
  }
}
