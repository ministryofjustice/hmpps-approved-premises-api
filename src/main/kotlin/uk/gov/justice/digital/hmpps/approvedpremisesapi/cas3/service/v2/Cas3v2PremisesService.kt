package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesSummaryResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2PremisesService(
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val cas3v2DomainEventService: Cas3v2DomainEventService,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val cas3PremisesCharacteristicRepository: Cas3PremisesCharacteristicRepository,
  private val cas3UserAccessService: Cas3UserAccessService,
) {
  fun getValidatedPremises(premisesId: UUID): CasResult<Cas3PremisesEntity> {
    val premises = cas3PremisesRepository.findByIdOrNull(premisesId)
    if (premises == null) return CasResult.NotFound("Cas3Premises", premisesId.toString())
    if (!cas3UserAccessService.currentUserCanViewPremises(premises.probationDeliveryUnit.probationRegion.id)) return CasResult.Unauthorised()
    return CasResult.Success(premises)
  }
  fun getPremises(premisesId: UUID): Cas3PremisesEntity? = cas3PremisesRepository.findByIdOrNull(premisesId)

  fun getAllPremisesSummaries(regionId: UUID, postcodeOrAddress: String?, premisesStatus: Cas3PremisesStatus): List<Cas3PremisesSummaryResult> {
    val postcodeOrAddressWithoutWhitespace = postcodeOrAddress?.filter { !it.isWhitespace() }
    return cas3PremisesRepository.findAllCas3PremisesSummary(regionId, postcodeOrAddress, postcodeOrAddressWithoutWhitespace, premisesStatus.value)
  }

  fun unarchivePremisesAndSaveDomainEvent(premises: Cas3PremisesEntity, restartDate: LocalDate, transactionId: UUID) {
    val currentStartDate = premises.startDate
    val currentEndDate = premises.endDate
    premises.startDate = restartDate
    premises.endDate = null
    premises.status = Cas3PremisesStatus.online
    cas3PremisesRepository.save(premises)
    cas3v2DomainEventService.savePremisesUnarchiveEvent(
      premises,
      currentStartDate,
      newStartDate = restartDate,
      currentEndDate,
      transactionId,
    )
  }

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

    validatePremises(
      probationDeliveryUnit?.probationRegion,
      localAuthorityAreaId,
      localAuthorityArea,
      probationDeliveryUnit,
      reference,
      addressLine1,
      postcode,
      turnaroundWorkingDays,
    ) {
      isUniqueName(reference = reference, probationDeliveryUnitId = probationDeliveryUnitId)
    }

    val validatedPremisesCharacteristics = getValidatedCharacteristics(characteristicIds)

    if (validationErrors.any()) {
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
      status = Cas3PremisesStatus.online,
      probationDeliveryUnit = probationDeliveryUnit!!,
      characteristics = validatedPremisesCharacteristics.toMutableList(),
      turnaroundWorkingDays = turnaroundWorkingDays ?: 2,
      bookings = mutableListOf(),
      bedspaces = mutableListOf(),
      startDate = LocalDate.now(),
      endDate = null,
      createdAt = OffsetDateTime.now(),
      lastUpdatedAt = null,
    )

    return success(cas3PremisesRepository.save(premises))
  }

  fun updatePremises(
    premisesId: UUID,
    reference: String,
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    characteristicIds: List<UUID>,
    notes: String?,
    probationDeliveryUnitId: UUID,
    turnaroundWorkingDays: Int,
  ): CasResult<Cas3PremisesEntity> = validatedCasResult {
    val premises = cas3PremisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("Cas3Premises", premisesId.toString())

    if (premises.probationDeliveryUnit.id != probationDeliveryUnitId) {
      return "$.probationDeliveryUnitId" hasSingleValidationError "premisesNotInProbationDeliveryUnit"
    }

    val localAuthorityArea = localAuthorityAreaId?.let { localAuthorityAreaRepository.findByIdOrNull(it) }
    val probationDeliveryUnit =
      probationDeliveryUnitRepository.findByIdAndProbationRegionId(probationDeliveryUnitId, probationRegionId)

    validatePremises(
      probationDeliveryUnit?.probationRegion,
      localAuthorityAreaId,
      localAuthorityArea,
      probationDeliveryUnit,
      reference,
      addressLine1,
      postcode,
      turnaroundWorkingDays,
    ) {
      isUniqueName(reference = reference, probationDeliveryUnitId = probationDeliveryUnitId)
    }
    val validatedPremisesCharacteristics = getValidatedCharacteristics(characteristicIds)

    if (validationErrors.any()) {
      return fieldValidationError
    }

    premises
      .apply {
        premises.name = reference
        premises.addressLine1 = addressLine1
        premises.addressLine2 = addressLine2
        premises.town = town
        premises.postcode = postcode
        premises.localAuthorityArea = localAuthorityArea
        premises.characteristics = validatedPremisesCharacteristics.toMutableList()
        premises.notes = notes.orEmpty()
        premises.probationDeliveryUnit = probationDeliveryUnit!!
        premises.turnaroundWorkingDays = turnaroundWorkingDays
        premises.lastUpdatedAt = OffsetDateTime.now()
      }

    return success(cas3PremisesRepository.save(premises))
  }

  private fun <T> CasResultValidatedScope<T>.getValidatedCharacteristics(premisesCharacteristicIds: List<UUID>): List<Cas3PremisesCharacteristicEntity> {
    val validatedCharacteristics =
      cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(premisesCharacteristicIds)

    val validatedCharacteristicIds = validatedCharacteristics.map { it.id }

    premisesCharacteristicIds.forEach {
      if (!validatedCharacteristicIds.contains(it)) {
        validationErrors["$.premisesCharacteristics[$it]"] = "doesNotExist"
      }
    }
    return validatedCharacteristics
  }

  private fun isUniqueName(reference: String, probationDeliveryUnitId: UUID): Boolean = !cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(reference, probationDeliveryUnitId)
}

@Suppress("LongParameterList")
fun <T> CasResultValidatedScope<T>.validatePremises(
  probationRegion: ProbationRegionEntity?,
  localAuthorityAreaId: UUID?,
  localAuthorityArea: LocalAuthorityAreaEntity?,
  probationDeliveryUnit: ProbationDeliveryUnitEntity?,
  reference: String,
  addressLine1: String,
  postcode: String,
  turnaroundWorkingDays: Int?,
  isUniqueName: () -> Boolean,
) {
  if (localAuthorityAreaId != null && localAuthorityArea == null) {
    "$.localAuthorityAreaId" hasValidationError "doesNotExist"
  }

  if (probationRegion == null) {
    "$.probationRegionId" hasValidationError "doesNotExist"
  }

  if (probationDeliveryUnit == null) {
    "$.probationDeliveryUnitId" hasValidationError "doesNotExist"
  }

  if (reference.isEmpty()) {
    "$.reference" hasValidationError "empty"
  } else if (!isUniqueName()) {
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
}
