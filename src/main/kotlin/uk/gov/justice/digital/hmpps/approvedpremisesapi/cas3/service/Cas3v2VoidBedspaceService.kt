package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3v2VoidBedspaceService(
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository,
) {
  fun findVoidBedspaces(premisesId: UUID): List<Cas3VoidBedspaceEntity> = cas3VoidBedspacesRepository.findActiveVoidBedspacesByPremisesId(premisesId)

  fun findVoidBedspace(premisesId: UUID, voidBedspaceId: UUID): Cas3VoidBedspaceEntity? = cas3VoidBedspacesRepository.findVoidBedspace(premisesId, voidBedspaceId)

  fun createVoidBedspace(
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    bedspace: Cas3BedspacesEntity,
  ): CasResult<Cas3VoidBedspaceEntity> = validatedCasResult {
    if (endDate.isBefore(startDate)) {
      "$.endDate" hasValidationError "beforeStartDate"
    }

    if (bedspace.endDate != null && startDate.isAfter(bedspace.endDate)) {
      "$.startDate" hasValidationError "voidStartDateAfterBedspaceEndDate"
    }
    if (bedspace.startDate != null && startDate.isBefore(bedspace.startDate)) {
      "$.startDate" hasValidationError "voidStartDateBeforeBedspaceStartDate"
    }
    if (bedspace.endDate != null && endDate.isAfter(bedspace.endDate)) {
      "$.endDate" hasValidationError "voidEndDateAfterBedspaceEndDate"
    }

    val reason = cas3VoidBedspaceReasonRepository.findByIdOrNull(reasonId)

    if (reason == null) {
      ("$.reason" hasValidationError "doesNotExist")
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val voidBedspacesEntity = cas3VoidBedspacesRepository.save(
      Cas3VoidBedspaceEntity(
        id = UUID.randomUUID(),
        startDate = startDate,
        endDate = endDate,
        bedspace = bedspace,
        reason = reason!!,
        referenceNumber = referenceNumber,
        notes = notes,
        cancellation = null,
        cancellationDate = null,
        cancellationNotes = null,
        bed = null,
        premises = null,
      ),
    )

    return success(voidBedspacesEntity)
  }
}
