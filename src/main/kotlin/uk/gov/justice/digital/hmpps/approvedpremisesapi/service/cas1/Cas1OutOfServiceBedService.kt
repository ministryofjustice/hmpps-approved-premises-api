package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.findAllByIdOrdered
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.EnumSet
import java.util.UUID

@Transactional
@Service
class Cas1OutOfServiceBedService(
  private val outOfServiceBedRepository: Cas1OutOfServiceBedRepository,
  private val outOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val outOfServiceBedCancellationRepository: Cas1OutOfServiceBedCancellationRepository,
  private val outOfServiceBedDetailsRepository: Cas1OutOfServiceBedRevisionRepository,
  private val userService: UserService,
) {
  fun createOutOfServiceBed(
    premises: ApprovedPremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    bedId: UUID,
  ) = createOutOfServiceBed(
    premises,
    startDate,
    endDate,
    reasonId,
    referenceNumber,
    notes,
    bedId,
    userService.getUserForRequest(),
  )

  fun createOutOfServiceBed(
    premises: ApprovedPremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    bedId: UUID,
    createdBy: UserEntity?,
  ): CasResult<Cas1OutOfServiceBedEntity> =
    validatedCasResult {
      if (endDate.isBefore(startDate)) {
        "$.endDate" hasValidationError "beforeStartDate"
      }

      val bed = premises.rooms.flatMap { it.beds }.firstOrNull { it.id == bedId }
      if (bed == null) {
        "$.bedId" hasValidationError "doesNotExist"
      }

      val reason = outOfServiceBedReasonRepository.findByIdOrNull(reasonId)
      if (reason == null) {
        "$.reason" hasValidationError "doesNotExist"
      }

      if (notes.isNullOrEmpty()) {
        "$.notes" hasValidationError "empty"
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val outOfServiceBed = outOfServiceBedRepository.saveAndFlush(
        Cas1OutOfServiceBedEntity(
          id = UUID.randomUUID(),
          premises = premises,
          bed = bed!!,
          createdAt = OffsetDateTime.now(),
          cancellation = null,
          revisionHistory = mutableListOf(),
        ),
      )

      outOfServiceBed.apply {
        revisionHistory += outOfServiceBedDetailsRepository.saveAndFlush(
          Cas1OutOfServiceBedRevisionEntity(
            id = UUID.randomUUID(),
            createdAt = this.createdAt,
            revisionType = Cas1OutOfServiceBedRevisionType.INITIAL,
            startDate = startDate,
            endDate = endDate,
            referenceNumber = referenceNumber,
            notes = notes,
            reason = reason!!,
            outOfServiceBed = this,
            createdBy = createdBy,
            changeTypePacked = Cas1OutOfServiceBedRevisionChangeType.NO_CHANGE,
          ),
        )
      }

      return success(outOfServiceBedRepository.saveAndFlush(outOfServiceBed))
    }

  fun updateOutOfServiceBed(
    outOfServiceBedId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
  ) = updateOutOfServiceBed(
    outOfServiceBedId,
    startDate,
    endDate,
    reasonId,
    referenceNumber,
    notes,
    userService.getUserForRequestOrNull(),
  )

  fun updateOutOfServiceBed(
    outOfServiceBedId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    createdBy: UserEntity?,
  ): CasResult<Cas1OutOfServiceBedEntity> {
    val outOfServiceBed = outOfServiceBedRepository.findByIdOrNull(outOfServiceBedId)
      ?: return CasResult.NotFound("OutOfServiceBed", outOfServiceBedId.toString())

    return validatedCasResult {
      if (endDate.isBefore(startDate)) {
        "$.endDate" hasValidationError "beforeStartDate"
      }

      val reason = outOfServiceBedReasonRepository.findByIdOrNull(reasonId)
      if (reason == null) {
        "$.reason" hasValidationError "doesNotExist"
      }

      if (notes.isNullOrEmpty()) {
        "$.notes" hasValidationError "empty"
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val details = outOfServiceBedDetailsRepository.saveAndFlush(
        Cas1OutOfServiceBedRevisionEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          revisionType = Cas1OutOfServiceBedRevisionType.UPDATE,
          startDate = startDate,
          endDate = endDate,
          referenceNumber = referenceNumber,
          notes = notes,
          reason = reason!!,
          outOfServiceBed = outOfServiceBed,
          createdBy = createdBy,
          changeTypePacked = getChangeType(
            outOfServiceBed.latestRevision,
            startDate,
            endDate,
            referenceNumber,
            notes,
            reason,
          ),
        ),
      )

      val updatedOutOfServiceBed = outOfServiceBedRepository.save(
        outOfServiceBed.apply {
          this.revisionHistory += details
        },
      )

      CasResult.Success(updatedOutOfServiceBed)
    }
  }

  private fun getChangeType(
    details: Cas1OutOfServiceBedRevisionEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    referenceNumber: String?,
    notes: String?,
    reason: Cas1OutOfServiceBedReasonEntity,
  ): Long {
    val result = EnumSet.noneOf(Cas1OutOfServiceBedRevisionChangeType::class.java)

    if (startDate != details.startDate) {
      result.add(Cas1OutOfServiceBedRevisionChangeType.START_DATE)
    }

    if (endDate != details.endDate) {
      result.add(Cas1OutOfServiceBedRevisionChangeType.END_DATE)
    }

    if (referenceNumber != details.referenceNumber) {
      result.add(Cas1OutOfServiceBedRevisionChangeType.REFERENCE_NUMBER)
    }

    if (notes != details.notes) {
      result.add(Cas1OutOfServiceBedRevisionChangeType.NOTES)
    }

    if (reason.id != details.reason.id) {
      result.add(Cas1OutOfServiceBedRevisionChangeType.REASON)
    }

    return Cas1OutOfServiceBedRevisionChangeType.pack(result)
  }

  fun cancelOutOfServiceBed(
    outOfServiceBed: Cas1OutOfServiceBedEntity,
    notes: String?,
  ) = validatedCasResult<Cas1OutOfServiceBedCancellationEntity> {
    if (outOfServiceBed.cancellation != null) {
      return generalError("This out-of-service bed has already been cancelled.")
    }

    val cancellationEntity = outOfServiceBedCancellationRepository.save(
      Cas1OutOfServiceBedCancellationEntity(
        id = UUID.randomUUID(),
        outOfServiceBed = outOfServiceBed,
        notes = notes,
        createdAt = OffsetDateTime.now(),
      ),
    )

    return success(cancellationEntity)
  }

  fun getOutOfServiceBedsForDate(
    temporality: Set<Temporality>,
    premisesId: UUID?,
    apAreaId: UUID?,
    date: LocalDate,
    pageCriteria: PageCriteria<Cas1OutOfServiceBedSortField>,
  ): Pair<List<Cas1OutOfServiceBedEntity>, PaginationMetadata?> {
    val sortFieldString = when (pageCriteria.sortBy) {
      Cas1OutOfServiceBedSortField.premisesName -> "p.name"
      Cas1OutOfServiceBedSortField.roomName -> "r.name"
      Cas1OutOfServiceBedSortField.bedName -> "b.name"
      Cas1OutOfServiceBedSortField.startDate -> "d.start_date"
      Cas1OutOfServiceBedSortField.endDate -> "d.end_date"
      Cas1OutOfServiceBedSortField.reason -> "oosr.name"
      Cas1OutOfServiceBedSortField.daysLost -> "(d.end_date - d.start_date)"
    }

    val excludePast = !temporality.contains(Temporality.past)
    val excludeCurrent = !temporality.contains(Temporality.current)
    val excludeFuture = !temporality.contains(Temporality.future)

    val page = outOfServiceBedRepository.findOutOfServiceBedIdsForDate(
      premisesId = premisesId,
      apAreaId = apAreaId,
      excludePast = excludePast,
      excludeCurrent = excludeCurrent,
      excludeFuture = excludeFuture,
      date = date,
      pageable = getPageableOrAllPages(pageCriteria.withSortBy(sortFieldString), unsafe = true),
    )

    val outOfServiceBeds = outOfServiceBedRepository.findAllByIdOrdered(page.content.map(UUID::fromString))

    return Pair(outOfServiceBeds, getMetadata(page, pageCriteria))
  }

  fun getOutOfServiceBeds(
    temporality: Set<Temporality>,
    premisesId: UUID?,
    apAreaId: UUID?,
    pageCriteria: PageCriteria<Cas1OutOfServiceBedSortField>,
  ): Pair<List<Cas1OutOfServiceBedEntity>, PaginationMetadata?> =
    getOutOfServiceBedsForDate(
      temporality = temporality,
      premisesId = premisesId,
      apAreaId = apAreaId,
      date = LocalDate.now(),
      pageCriteria = pageCriteria,
    )

  fun getActiveOutOfServiceBedsForPremisesId(premisesId: UUID) = outOfServiceBedRepository.findAllActiveForPremisesId(premisesId)

  fun getCurrentOutOfServiceBedsCountForPremisesId(premisesId: UUID): Int {
    return outOfServiceBedRepository.findOutOfServiceBedIdsForDate(
      premisesId = premisesId,
      apAreaId = null,
      excludePast = true,
      excludeCurrent = false,
      excludeFuture = true,
      date = LocalDate.now(),
      pageable = Pageable.unpaged(),
    ).size
  }

  fun getOutOfServiceBedWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ): Cas1OutOfServiceBedEntity? {
    val outOfServiceBedId = outOfServiceBedRepository.findByBedIdAndOverlappingDate(
      bedId,
      startDate,
      endDate,
      thisEntityId,
    ).firstOrNull() ?: return null

    return outOfServiceBedRepository.findByIdOrNull(UUID.fromString(outOfServiceBedId))
  }
}
