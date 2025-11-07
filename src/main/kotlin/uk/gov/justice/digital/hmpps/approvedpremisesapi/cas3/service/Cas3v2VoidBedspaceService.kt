package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2VoidBedspaceService(
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository,
  private val cas3BookingService: Cas3v2BookingService,
) {
  fun findVoidBedspaces(premisesId: UUID): List<Cas3VoidBedspaceEntity> = cas3VoidBedspacesRepository.findActiveVoidBedspacesByPremisesId(premisesId)

  fun findVoidBedspace(premisesId: UUID, bedspaceId: UUID, voidBedspaceId: UUID): Cas3VoidBedspaceEntity? = cas3VoidBedspacesRepository.findVoidBedspace(premisesId, bedspaceId, voidBedspaceId)

  @Transactional
  fun createVoidBedspace(
    voidBedspaceStartDate: LocalDate,
    voidBedspaceEndDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    bedspace: Cas3BedspacesEntity,
    costCentre: Cas3CostCentre?,
  ): CasResult<Cas3VoidBedspaceEntity> = validatedCasResult {
    val reason = cas3VoidBedspaceReasonRepository.findByIdOrNull(reasonId)

    cas3BookingService.throwIfVoidBedspaceDatesConflict(voidBedspaceStartDate, voidBedspaceEndDate, null, bedspace.id)

    if (validateVoidBedspaceDetails(bedspace, voidBedspaceStartDate, voidBedspaceEndDate, reason).any()) {
      return fieldValidationError
    }

    val voidBedspacesEntity = cas3VoidBedspacesRepository.save(
      Cas3VoidBedspaceEntity(
        id = UUID.randomUUID(),
        startDate = voidBedspaceStartDate,
        endDate = voidBedspaceEndDate,
        bedspace = bedspace,
        reason = reason!!,
        referenceNumber = referenceNumber,
        notes = notes,
        cancellation = null,
        cancellationDate = null,
        cancellationNotes = null,
        bed = null,
        premises = null,
        costCentre = costCentre,
      ),
    )

    return success(voidBedspacesEntity)
  }

  @Transactional
  fun cancelVoidBedspace(
    voidBedspace: Cas3VoidBedspaceEntity,
    notes: String?,
  ) = validatedCasResult<Cas3VoidBedspaceEntity> {
    if (voidBedspace.cancellationDate != null) {
      return generalError("This Void Bedspace already has a cancellation set")
    }

    voidBedspace.cancellationDate = OffsetDateTime.now()
    voidBedspace.cancellationNotes = notes

    val cancellationEntity = cas3VoidBedspacesRepository.save(
      voidBedspace,
    )

    return success(cancellationEntity)
  }

  @Transactional
  fun updateVoidBedspace(
    voidBedspaceEntity: Cas3VoidBedspaceEntity,
    voidBedspaceStartDate: LocalDate,
    voidBedspaceEndDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
  ): CasResult<Cas3VoidBedspaceEntity> = validatedCasResult {
    val bedspace = voidBedspaceEntity.bedspace!!
    val reason = cas3VoidBedspaceReasonRepository.findByIdOrNull(reasonId)

    if (voidBedspaceEntity.cancellationDate != null) {
      return generalError("This Void Bedspace has been cancelled")
    }

    cas3BookingService.throwIfVoidBedspaceDatesConflict(voidBedspaceStartDate, voidBedspaceEndDate, null, bedspace.id, voidBedspaceEntity.id)

    if (validateVoidBedspaceDetails(bedspace, voidBedspaceStartDate, voidBedspaceEndDate, reason).any()) {
      return fieldValidationError
    }

    val updatedVoidBedspaceEntity = cas3VoidBedspacesRepository.save(
      voidBedspaceEntity.apply {
        this.startDate = voidBedspaceStartDate
        this.endDate = voidBedspaceEndDate
        this.reason = reason!!
        this.referenceNumber = referenceNumber
        this.notes = notes
      },
    )

    return success(updatedVoidBedspaceEntity)
  }

  private fun CasResultValidatedScope<Cas3VoidBedspaceEntity>.validateVoidBedspaceDetails(
    bedspace: Cas3BedspacesEntity,
    voidBedspaceStartDate: LocalDate,
    voidBedspaceEndDate: LocalDate,
    reason: Cas3VoidBedspaceReasonEntity?,
  ): ValidationErrors {
    cas3BookingService.throwIfBookingDatesConflict(voidBedspaceStartDate, voidBedspaceEndDate, null, bedspace.id)

    if (voidBedspaceEndDate.isBefore(voidBedspaceStartDate)) {
      "$.endDate" hasValidationError "voidEndDateBeforeVoidStartDate"
    }
    if (bedspace.endDate != null && voidBedspaceStartDate.isAfter(bedspace.endDate)) {
      "$.startDate" hasValidationError "voidStartDateAfterBedspaceEndDate"
    }
    if (bedspace.startDate != null && voidBedspaceStartDate.isBefore(bedspace.startDate)) {
      "$.startDate" hasValidationError "voidStartDateBeforeBedspaceStartDate"
    }
    if (bedspace.endDate != null && voidBedspaceEndDate.isAfter(bedspace.endDate)) {
      "$.endDate" hasValidationError "voidEndDateAfterBedspaceEndDate"
    }
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    }

    return validationErrors
  }
}
