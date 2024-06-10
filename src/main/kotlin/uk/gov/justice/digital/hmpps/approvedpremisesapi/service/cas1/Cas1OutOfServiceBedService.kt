package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1OutOfServiceBedService(
  private val outOfServiceBedRepository: Cas1OutOfServiceBedRepository,
  private val outOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val outOfServiceBedCancellationRepository: Cas1OutOfServiceBedCancellationRepository,
) {
  fun createOutOfServiceBed(
    premises: ApprovedPremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    bedId: UUID,
  ): ValidatableActionResult<Cas1OutOfServiceBedEntity> =
    validated {
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

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val cas1outOfServiceBedsEntity = outOfServiceBedRepository.save(
        Cas1OutOfServiceBedEntity(
          id = UUID.randomUUID(),
          premises = premises,
          reason = reason!!,
          bed = bed!!,
          createdAt = OffsetDateTime.now(),
          startDate = startDate,
          endDate = endDate,
          referenceNumber = referenceNumber,
          notes = notes,
          cancellation = null,
        ),
      )

      return success(cas1outOfServiceBedsEntity)
    }

  fun updateOutOfServiceBed(
    outOfServiceBedId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
  ): AuthorisableActionResult<ValidatableActionResult<Cas1OutOfServiceBedEntity>> {
    val outOfServiceBed = outOfServiceBedRepository.findByIdOrNull(outOfServiceBedId)
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(
      validated {
        if (endDate.isBefore(startDate)) {
          "$.endDate" hasValidationError "beforeStartDate"
        }

        val reason = outOfServiceBedReasonRepository.findByIdOrNull(reasonId)
        if (reason == null) {
          "$.reason" hasValidationError "doesNotExist"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        val updatedCas1OutOfServiceBedsEntity = outOfServiceBedRepository.save(
          outOfServiceBed.apply {
            this.startDate = startDate
            this.endDate = endDate
            this.reason = reason!!
            this.referenceNumber = referenceNumber
            this.notes = notes
          },
        )

        success(updatedCas1OutOfServiceBedsEntity)
      },
    )
  }

  fun cancelOutOfServiceBed(
    outOfServiceBed: Cas1OutOfServiceBedEntity,
    notes: String?,
  ) = validated<Cas1OutOfServiceBedCancellationEntity> {
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

  fun getOutOfServiceBeds(
    temporality: Set<Temporality>,
    premisesId: UUID?,
    apAreaId: UUID?,
    pageCriteria: PageCriteria<Cas1OutOfServiceBedSortField>,
  ): Pair<List<Cas1OutOfServiceBedEntity>, PaginationMetadata?> {
    val sortFieldString = when (pageCriteria.sortBy) {
      Cas1OutOfServiceBedSortField.premisesName -> "premises.name"
      Cas1OutOfServiceBedSortField.roomName -> "bed.room.name"
      Cas1OutOfServiceBedSortField.bedName -> "bed.name"
      Cas1OutOfServiceBedSortField.outOfServiceFrom -> "startDate"
      Cas1OutOfServiceBedSortField.outOfServiceTo -> "endDate"
      Cas1OutOfServiceBedSortField.reason -> "reason.name"
      Cas1OutOfServiceBedSortField.daysLost -> "(oosb.endDate - oosb.startDate)"
    }

    val excludePast = !temporality.contains(Temporality.past)
    val excludeCurrent = !temporality.contains(Temporality.current)
    val excludeFuture = !temporality.contains(Temporality.future)

    val page = outOfServiceBedRepository.findOutOfServiceBeds(
      premisesId,
      apAreaId,
      excludePast,
      excludeCurrent,
      excludeFuture,
      getPageableOrAllPages(pageCriteria.withSortBy(sortFieldString), unsafe = true),
    )

    return Pair(page.content, getMetadata(page, pageCriteria))
  }

  fun getActiveOutOfServiceBedsForPremisesId(premisesId: UUID) = outOfServiceBedRepository.findAllActiveForPremisesId(premisesId)

  fun getOutOfServiceBedWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) = outOfServiceBedRepository.findByBedIdAndOverlappingDate(
    bedId,
    startDate,
    endDate,
    thisEntityId,
  ).firstOrNull()
}
