package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_DAYS_CREATE_BEDSPACE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_LENGTH_BEDSPACE_REFERENCE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Cas3FieldValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2BedspacesService(
  private val characteristicService: CharacteristicService,
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3PremisesService: Cas3v2PremisesService,
) {
  @SuppressWarnings("MagicNumber")
  fun createBedspace(
    premises: Cas3PremisesEntity,
    bedspaceReference: String,
    startDate: LocalDate,
    notes: String?,
    characteristicIds: List<UUID>,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val trimmedReference = bedspaceReference.trim()
    var bedspace = Cas3BedspacesEntity(
      id = UUID.randomUUID(),
      reference = trimmedReference,
      notes = notes,
      premises = premises,
      characteristics = mutableListOf(),
      startDate = startDate,
      endDate = null,
      createdAt = OffsetDateTime.now(),
    )

    if (isValidBedspaceReference(trimmedReference) &&
      premises.bedspaces.any { bedspace -> bedspace.reference.equals(trimmedReference, ignoreCase = true) }
    ) {
      "$.reference" hasValidationError "bedspaceReferenceExists"
    }

    if (startDate.isBefore(LocalDate.now().minusDays(MAX_DAYS_CREATE_BEDSPACE))) {
      "$.startDate" hasValidationError "invalidStartDateInThePast"
    }

    if (startDate.isAfter(LocalDate.now().plusDays(MAX_DAYS_CREATE_BEDSPACE))) {
      "$.startDate" hasValidationError "invalidStartDateInTheFuture"
    }

    if (startDate.isBefore(premises.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.startDate" to Cas3ValidationMessage(
            entityId = premises.id.toString(),
            message = "startDateBeforePremisesStartDate",
            value = premises.startDate.toString(),
          ),
        ),
      )
    }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, validationErrors)
    if (validationErrors.any()) {
      return fieldValidationError
    }
    bedspace.characteristics.addAll(characteristicEntities.filterNotNull())
    bedspace = cas3BedspacesRepository.save(bedspace)

    if (premises.isPremisesScheduledToArchive()) {
      cas3PremisesService.unarchivePremisesAndSaveDomainEvent(premises, startDate)
    }

    return success(bedspace)
  }

  private fun getAndValidateCharacteristics(
    characteristicIds: List<UUID>,
    validationErrors: ValidationErrors,
  ): List<Cas3BedspaceCharacteristicEntity?> = characteristicIds.mapIndexed { index, id ->
    val entity = characteristicService.getCas3BedspaceCharacteristic(id)
    if (entity == null) {
      validationErrors["$.characteristics[$index]"] = "doesNotExist"
    }
    entity
  }

  private fun CasResultValidatedScope<Cas3BedspacesEntity>.isValidBedspaceReference(
    trimmedReference: String,
  ): Boolean {
    if (trimmedReference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    } else {
      if (trimmedReference.length < MAX_LENGTH_BEDSPACE_REFERENCE) {
        "$.reference" hasValidationError "bedspaceReferenceNotMeetMinimumLength"
      }

      if (!trimmedReference.any { it.isLetterOrDigit() }) {
        "$.reference" hasValidationError "bedspaceReferenceMustIncludeLetterOrNumber"
      }
    }
    return !validationErrors.any()
  }
}
