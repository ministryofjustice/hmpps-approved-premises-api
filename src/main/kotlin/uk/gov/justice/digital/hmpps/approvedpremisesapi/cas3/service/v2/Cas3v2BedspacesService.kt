package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveActions
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService.Companion.MAX_DAYS_CREATE_BEDSPACE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService.Companion.MAX_LENGTH_BEDSPACE_REFERENCE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Cas3FieldValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2BedspacesService(
  private val characteristicService: CharacteristicService,
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3PremisesService: Cas3v2PremisesService,
  private val cas3v2DomainEventService: Cas3v2DomainEventService,
  private val objectMapper: ObjectMapper,
) {

  fun getBedspace(premisesId: UUID, bedspaceId: UUID): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findCas3Bedspace(premisesId, bedspaceId) ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())
    return success(bedspace)
  }

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
      createdDate = startDate,
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
      val domainEventTransactionId = UUID.randomUUID()
      cas3PremisesService.unarchivePremisesAndSaveDomainEvent(premises, startDate, domainEventTransactionId)
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

  fun getPremisesBedspaces(premisesId: UUID): List<Cas3BedspacesEntity> = cas3BedspacesRepository.findByPremisesId(premisesId)

  fun getBedspaceArchiveHistory(bedspaceId: UUID): CasResult<List<Cas3BedspaceArchiveAction>> = validatedCasResult {
    val domainEvents = cas3v2DomainEventService.getBedspaceActiveDomainEvents(
      bedspaceId,
      listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED),
    )
    return when {
      domainEvents.any() -> getBedspaceArchiveActions(domainEvents)
      else -> success(emptyList())
    }
  }

  fun getBedspacesArchiveHistory(bedspaceIds: List<UUID>): List<Cas3BedspaceArchiveActions> {
    val domainEvents = cas3v2DomainEventService.getBedspacesActiveDomainEvents(
      bedspaceIds,
      listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED),
    )
    return bedspaceIds.map { bedspaceId ->
      val bedspaceDomainEvents = domainEvents.filter { it.cas3BedspaceId == bedspaceId }
      val actions = extractEntityFromCasResult(getBedspaceArchiveActions(bedspaceDomainEvents))
      Cas3BedspaceArchiveActions(bedspaceId, actions)
    }
  }

  private fun getBedspaceArchiveActions(domainEvents: List<DomainEventEntity>): CasResult<List<Cas3BedspaceArchiveAction>> {
    val today = LocalDate.now()

    val archiveHistory = domainEvents
      .mapNotNull { domainEventEntity ->
        when (domainEventEntity.type) {
          DomainEventType.CAS3_BEDSPACE_UNARCHIVED -> {
            val eventDetails =
              objectMapper.readValue(domainEventEntity.data, CAS3BedspaceUnarchiveEvent::class.java).eventDetails
            val restartDate = eventDetails.newStartDate
            if (restartDate <= today) {
              Cas3BedspaceArchiveAction(
                status = Cas3BedspaceStatus.online,
                date = restartDate,
              )
            } else {
              null
            }
          }

          DomainEventType.CAS3_BEDSPACE_ARCHIVED -> {
            val eventDetails =
              objectMapper.readValue(domainEventEntity.data, CAS3BedspaceArchiveEvent::class.java).eventDetails
            val endDate = eventDetails.endDate
            if (endDate <= today) {
              Cas3BedspaceArchiveAction(
                status = Cas3BedspaceStatus.archived,
                date = endDate,
              )
            } else {
              null
            }
          }

          else -> return CasResult.GeneralValidationError("Incorrect domain event type for archive history: ${domainEventEntity.type}, ${domainEventEntity.id}")
        }
      }.sortedBy { it.date }

    return CasResult.Success(archiveHistory)
  }

  fun getBedspaceStatus(bedspace: Cas3BedspacesEntity) = BedspaceStatusHelper.getBedspaceStatus(bedspace.startDate, bedspace.endDate)

  fun updateBedspace(
    premises: Cas3PremisesEntity,
    bedspaceId: UUID,
    bedspaceReference: String,
    notes: String?,
    characteristicIds: List<UUID>,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findCas3Bedspace(premises.id, bedspaceId) ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())
    val trimmedReference = bedspaceReference.trim()
    if (isValidBedspaceReference(trimmedReference) &&
      premises.bedspaces.any { bedspace -> bedspace.reference.equals(bedspaceReference, ignoreCase = true) }
    ) {
      "$.reference" hasValidationError "bedspaceReferenceExists"
    }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, validationErrors)
    if (validationErrors.any()) {
      return fieldValidationError
    }
    bedspace.characteristics = characteristicEntities.filterNotNull().toMutableList()
    bedspace.reference = trimmedReference
    bedspace.notes = notes

    val updatedBedspace = cas3BedspacesRepository.save(bedspace)

    return success(updatedBedspace)
  }

  fun findBedspace(premisesId: UUID, bedspaceId: UUID): Cas3BedspacesEntity? = cas3BedspacesRepository.findCas3Bedspace(premisesId = premisesId, bedspaceId = bedspaceId)
}
