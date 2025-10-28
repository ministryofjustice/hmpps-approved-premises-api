package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_DAYS_ARCHIVE_BEDSPACE_IN_PAST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_DAYS_ARCHIVE_PREMISES_IN_PAST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_DAYS_UNARCHIVE_BEDSPACE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_DAYS_UNARCHIVE_PREMISES
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_MONTHS_ARCHIVE_BEDSPACE_IN_FUTURE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Cas3FieldValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceActive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceArchived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceOnline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceUpcoming
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("TooManyFunctions")
@Service
class Cas3v2ArchiveService(
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val cas3v2BookingRepository: Cas3v2BookingRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val domainEventRepository: DomainEventRepository,
  private val cas3v2DomainEventService: Cas3v2DomainEventService,
  private val workingDayService: WorkingDayService,
  private val objectMapper: ObjectMapper,
  private val clock: Clock,
) {

  @Transactional
  fun archiveBedspace(
    bedspaceId: UUID,
    premises: Cas3PremisesEntity,
    endDate: LocalDate,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findByIdOrNull(bedspaceId)
      ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    if (endDate.isBefore(LocalDate.now(clock).minusDays(MAX_DAYS_ARCHIVE_BEDSPACE_IN_PAST))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInThePast"
    }

    if (endDate.isAfter(LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_BEDSPACE_IN_FUTURE))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInTheFuture"
    }

    if (endDate.isBefore(bedspace.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.endDate" to Cas3ValidationMessage(
            entityId = bedspace.id.toString(),
            message = "endDateBeforeBedspaceStartDate",
            value = bedspace.startDate.toString(),
          ),
        ),
      )
    }

    cas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED))
      .sortedByDescending { it.createdAt }
      .asSequence()
      .map { objectMapper.readValue(it.data, CAS3BedspaceArchiveEvent::class.java).eventDetails.endDate }
      .firstOrNull { it >= endDate }
      ?.let { archiveDate ->
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = bedspace.id.toString(),
              message = "endDateOverlapPreviousBedspaceArchiveEndDate",
              value = archiveDate.toString(),
            ),
          ),
        )
      }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val domainEventTransactionId = UUID.randomUUID()

    canArchiveBedspace(bedspaceId, endDate)?.let { return it }

    val updatedBedspace = archiveBedspaceAndSaveDomainEvent(bedspace, premises.id, endDate, domainEventTransactionId)

    archivePremisesIfAllBedspacesArchived(premises, domainEventTransactionId)

    return success(updatedBedspace)
  }

  fun unarchiveBedspace(
    premises: Cas3PremisesEntity,
    bedspaceId: UUID,
    restartDate: LocalDate,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findCas3Bedspace(premises.id, bedspaceId) ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "doesNotExist"

    if (!isCas3BedspaceArchived(bedspace.endDate)) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotArchived"
    }

    val today = LocalDate.now()

    if (restartDate.isBefore(today.minusDays(MAX_DAYS_UNARCHIVE_BEDSPACE))) {
      "$.restartDate" hasValidationError "invalidRestartDateInThePast"
    }

    if (restartDate.isAfter(today.plusDays(MAX_DAYS_UNARCHIVE_BEDSPACE))) {
      "$.restartDate" hasValidationError "invalidRestartDateInTheFuture"
    }

    if (restartDate.isBefore(bedspace.endDate)) {
      "$.restartDate" hasValidationError "beforeLastBedspaceArchivedDate"
    }

    if (hasErrors()) {
      return@validatedCasResult errors()
    }

    val domainEventTransactionId = UUID.randomUUID()

    val unarchivedBedspace = unarchiveBedspaceAndSaveDomainEvent(bedspace, premises.id, restartDate, domainEventTransactionId)

    if (premises.status == Cas3PremisesStatus.archived) {
      unarchivePremisesAndSaveDomainEvent(premises, restartDate, domainEventTransactionId)
    }

    success(unarchivedBedspace)
  }

  fun canArchivePremisesInFuture(premisesId: UUID): Cas3ValidationResults {
    val maximumPremisesArchiveDate = LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE)
    var affectedBedspaces = mutableListOf<Cas3ValidationResult>()

    val overlapBookings = cas3v2BookingRepository.findActiveOverlappingBookingByPremisesId(premisesId, LocalDate.now(clock))

    overlapBookings.map {
      val bookingTurnaround = workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0)
      if (bookingTurnaround >= maximumPremisesArchiveDate) {
        affectedBedspaces.add(
          Cas3ValidationResult(
            entityId = it.bedspace.id,
            entityReference = it.bedspace.reference,
            date = bookingTurnaround,
          ),
        )
      }
    }

    val overlappingVoids = cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateByPremisesIdV2(premisesId, maximumPremisesArchiveDate)

    overlappingVoids.map {
      affectedBedspaces.add(
        Cas3ValidationResult(
          entityId = it.bedspace!!.id,
          entityReference = it.bedspace!!.reference,
          date = it.endDate,
        ),
      )
    }

    affectedBedspaces = affectedBedspaces
      .groupBy { it.entityId }
      .mapValues { it.value.sortedByDescending { it.date }.take(1) }
      .map { it.value.first() }
      .toMutableList()

    return Cas3ValidationResults(
      items = affectedBedspaces,
    )
  }

  @Suppress("CyclomaticComplexMethod")
  @Transactional
  fun archivePremises(
    premises: Cas3PremisesEntity,
    archivePremisesEndDate: LocalDate,
  ): CasResult<Cas3PremisesEntity> = validatedCasResult {
    if (archivePremisesEndDate.isBefore(LocalDate.now().minusDays(MAX_DAYS_ARCHIVE_PREMISES_IN_PAST))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInThePast"
    }

    if (archivePremisesEndDate.isAfter(LocalDate.now().plusMonths(MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInTheFuture"
    }

    if (archivePremisesEndDate.isBefore(premises.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.endDate" to Cas3ValidationMessage(
            entityId = premises.id.toString(),
            message = "endDateBeforePremisesStartDate",
            value = premises.startDate.toString(),
          ),
        ),
      )
    }

    cas3v2DomainEventService.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED))
      .sortedByDescending { it.createdAt }
      .asSequence()
      .map { objectMapper.readValue(it.data, CAS3PremisesArchiveEvent::class.java).eventDetails.endDate }
      .firstOrNull { it >= archivePremisesEndDate }
      ?.let { archiveDate ->
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = premises.id.toString(),
              message = "endDateOverlapPreviousPremisesArchiveEndDate",
              value = archiveDate.toString(),
            ),
          ),
        )
      }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val activeBedspaces = cas3BedspacesRepository.findByPremisesId(premises.id)
      .asSequence()
      .filter {
        (
          isCas3BedspaceOnline(
            it.startDate,
            it.endDate,
          ) ||
            isCas3BedspaceUpcoming(it.startDate)
          ) &&
          isCas3BedspaceActive(it.endDate, archivePremisesEndDate)
      }

    if (activeBedspaces.any()) {
      val lastUpcomingBedspace = activeBedspaces.maxByOrNull { it.startDate!! }

      if (lastUpcomingBedspace != null && lastUpcomingBedspace.startDate!! > archivePremisesEndDate) {
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = lastUpcomingBedspace.id.toString(),
              message = "existingUpcomingBedspace",
              value = lastUpcomingBedspace.startDate!!.plusDays(1).toString(),
            ),
          ),
        )
      }

      canArchivePremisesBedspaces(premises.id, archivePremisesEndDate)?.let {
        return Cas3FieldValidationError(it.validationMessages.entries.associate { entry -> entry.key to entry.value })
      }
    }

    val domainEventTransactionId = UUID.randomUUID()

    // archive premises
    val archivedPremises = archivePremisesAndSaveDomainEvent(premises, archivePremisesEndDate, domainEventTransactionId)

    if (activeBedspaces.any()) {
      // archive all online bedspaces
      activeBedspaces.forEach { bedspace ->
        archiveBedspaceAndSaveDomainEvent(bedspace, premises.id, archivePremisesEndDate, domainEventTransactionId)
      }
    }

    return success(archivedPremises)
  }

  fun unarchivePremises(
    premises: Cas3PremisesEntity,
    restartDate: LocalDate,
  ): CasResult<Cas3PremisesEntity> = validatedCasResult {
    if (!premises.isPremisesArchived()) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotArchived"
    }

    val today = LocalDate.now()

    if (restartDate.isBefore(today.minusDays(MAX_DAYS_UNARCHIVE_PREMISES))) {
      "$.restartDate" hasValidationError "invalidRestartDateInThePast"
    }

    if (restartDate.isAfter(today.plusDays(MAX_DAYS_UNARCHIVE_PREMISES))) {
      "$.restartDate" hasValidationError "invalidRestartDateInTheFuture"
    }

    if (restartDate.isBefore(premises.endDate)) {
      "$.restartDate" hasValidationError "beforeLastPremisesArchivedDate"
    }

    if (hasErrors()) {
      return@validatedCasResult errors()
    }

    val domainEventTransactionId = UUID.randomUUID()

    val unarchivePremises = unarchivePremisesAndSaveDomainEvent(premises, restartDate, domainEventTransactionId)

    val bedspaces = cas3BedspacesRepository.findByPremisesId(premises.id)
    val uniqueBedspaces = bedspaces.groupBy { b -> b.reference }
    uniqueBedspaces.forEach { bedspaces ->
      val lastBedspace = bedspaces.value.sortedByDescending { it.createdAt }.first()
      unarchiveBedspaceAndSaveDomainEvent(lastBedspace, premises.id, restartDate, domainEventTransactionId)
    }

    success(unarchivePremises)
  }

  @Transactional
  fun cancelScheduledUnarchiveBedspace(
    bedspaceId: UUID,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findByIdOrNull(bedspaceId)
      ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "doesNotExist"

    if (isCas3BedspaceOnline(startDate = bedspace.startDate, endDate = bedspace.endDate)) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceAlreadyOnline"
    }

    val latestBedspaceUnarchiveDomainEvent = domainEventRepository.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(
      bedspace.id,
      CAS3_BEDSPACE_UNARCHIVED,
    ) ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotScheduledToUnarchive"

    domainEventRepository.save(
      latestBedspaceUnarchiveDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    val eventDetails = objectMapper.readValue(latestBedspaceUnarchiveDomainEvent.data, CAS3BedspaceUnarchiveEvent::class.java).eventDetails

    val updatedBedspace = cas3BedspacesRepository.save(
      bedspace.copy(
        startDate = eventDetails.currentStartDate,
        endDate = eventDetails.currentEndDate,
      ),
    )

    success(updatedBedspace)
  }

  @Transactional
  fun cancelScheduledArchiveBedspace(
    premises: Cas3PremisesEntity,
    bedspaceId: UUID,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findCas3Bedspace(premises.id, bedspaceId) ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "doesNotExist"

    // Check if bedspace not scheduled to archive
    if (bedspace.endDate == null) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotScheduledToArchive"
    }

    // Check if bedspace is already archived
    if (isCas3BedspaceArchived(bedspace.endDate)) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceAlreadyArchived"
    }

    val latestBedspaceArchiveDomainEvent = domainEventRepository.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(
      bedspace.id,
      CAS3_BEDSPACE_ARCHIVED,
    ) ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotScheduledToArchive"

    if (latestBedspaceArchiveDomainEvent.cas3TransactionId == null) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceArchiveDomainEventIncorrect"
    }

    if (premises.endDate != null && premises.endDate!!.isAfter(LocalDate.now(clock))) {
      domainEventRepository.findByCas3TransactionIdAndType(latestBedspaceArchiveDomainEvent.cas3TransactionId!!, CAS3_PREMISES_ARCHIVED).let {
        // Premises scheduled to archive, cancel scheduled premises and bedspaces set to archive
        val result = cancelScheduledArchivePremises(premises.id)
        if (result is CasResult.FieldValidationError) {
          return Cas3FieldValidationError(result.validationMessages as Map<String, Cas3ValidationMessage>)
        }

        return success(bedspace)
      }
    }

    domainEventRepository.save(
      latestBedspaceArchiveDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    val bedspaceArchiveDomainEventData = objectMapper.readValue(latestBedspaceArchiveDomainEvent.data, CAS3BedspaceArchiveEvent::class.java)

    // Update the bedspace to cancel a scheduled archive
    val updatedBedspace = cas3BedspacesRepository.save(
      bedspace.copy(
        endDate = bedspaceArchiveDomainEventData.eventDetails.currentEndDate,
      ),
    )

    success(updatedBedspace)
  }

  @Transactional
  fun cancelScheduledArchivePremises(
    premisesId: UUID,
  ): CasResult<Cas3PremisesEntity> = validatedCasResult {
    val premises = cas3PremisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("Premises", premisesId.toString())

    if (premises.endDate == null) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotScheduledToArchive"
    }

    if (premises.endDate!! <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesAlreadyArchived"
    }

    val latestPremisesArchiveDomainEvent = domainEventRepository.findLastCas3PremisesActiveDomainEventByPremisesIdAndType(
      premisesId,
      CAS3_PREMISES_ARCHIVED,
    ) ?: return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotScheduledToArchive"

    val premisesArchiveDomainEventData = objectMapper.readValue(latestPremisesArchiveDomainEvent.data, CAS3PremisesArchiveEvent::class.java)

    if (premisesArchiveDomainEventData.eventDetails.endDate <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesArchiveDateInThePast"
    }

    if (latestPremisesArchiveDomainEvent.cas3TransactionId == null) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesArchiveDomainEventIncorrect"
    }

    domainEventRepository.findByCas3TransactionIdAndType(latestPremisesArchiveDomainEvent.cas3TransactionId!!, CAS3_BEDSPACE_ARCHIVED).forEach { bedspaceDomainEvent ->
      val bedspaceArchiveDomainEventData =
        objectMapper.readValue(bedspaceDomainEvent.data, CAS3BedspaceArchiveEvent::class.java)

      val bedspace = cas3BedspacesRepository.findByIdOrNull(bedspaceDomainEvent.cas3BedspaceId!!) ?: return CasResult.NotFound("Bedspace", bedspaceDomainEvent.cas3BedspaceId!!.toString())
      cas3BedspacesRepository.save(
        bedspace.copy(
          endDate = bedspaceArchiveDomainEventData.eventDetails.currentEndDate,
        ),
      )

      domainEventRepository.save(
        bedspaceDomainEvent.copy(
          cas3CancelledAt = OffsetDateTime.now(clock),
        ),
      )
    }

    domainEventRepository.save(
      latestPremisesArchiveDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    premises.endDate = null
    premises.status = Cas3PremisesStatus.online

    return success(cas3PremisesRepository.save(premises))
  }

  @Transactional
  fun cancelScheduledUnarchivePremises(
    premisesId: UUID,
  ): CasResult<Cas3PremisesEntity> = validatedCasResult {
    val premises = cas3PremisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("Premises", premisesId.toString())

    if (premises.status == Cas3PremisesStatus.archived || (premises.endDate != null && premises.endDate!! <= LocalDate.now(clock))) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesAlreadyArchived"
    }

    val latestUnarchivePremisesDomainEvent = domainEventRepository.findLastCas3PremisesActiveDomainEventByPremisesIdAndType(
      premisesId,
      CAS3_PREMISES_UNARCHIVED,
    ) ?: return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotScheduledToUnarchive"

    val unarchivePremisesDomainEventDetails = objectMapper.readValue(latestUnarchivePremisesDomainEvent.data, CAS3PremisesUnarchiveEvent::class.java).eventDetails

    if (unarchivePremisesDomainEventDetails.newStartDate <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesUnarchiveDateInThePast"
    }

    domainEventRepository.save(
      latestUnarchivePremisesDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    premises.startDate = unarchivePremisesDomainEventDetails.currentStartDate
    premises.endDate = unarchivePremisesDomainEventDetails.currentEndDate
    premises.status = Cas3PremisesStatus.archived

    val updatedPremises = cas3PremisesRepository.save(premises)

    val bedspaces = cas3BedspacesRepository.findByPremisesId(premises.id)
    bedspaces.forEach { bedspace ->
      val latestBedspaceUnarchiveDomainEvent = domainEventRepository.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(
        bedspace.id,
        CAS3_BEDSPACE_UNARCHIVED,
      )

      if (latestBedspaceUnarchiveDomainEvent != null) {
        domainEventRepository.save(
          latestBedspaceUnarchiveDomainEvent.copy(
            cas3CancelledAt = OffsetDateTime.now(clock),
          ),
        )

        val bedspaceUnarchiveEventDetails = objectMapper.readValue(latestBedspaceUnarchiveDomainEvent.data, CAS3BedspaceUnarchiveEvent::class.java).eventDetails
        val previousBedspaceStartDate = bedspaceUnarchiveEventDetails.currentStartDate
        val previousBedspaceEndDate = bedspaceUnarchiveEventDetails.currentEndDate

        cas3BedspacesRepository.save(
          bedspace.copy(
            startDate = previousBedspaceStartDate,
            endDate = previousBedspaceEndDate,
          ),
        )
      }
    }

    success(updatedPremises)
  }

  fun canArchiveBedspaceInFuture(premisesId: UUID, bedspaceId: UUID): CasResult<Cas3ValidationResult?> {
    val bedspacesEntity = cas3BedspacesRepository.findCas3Bedspace(premisesId, bedspaceId) ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    val threeMonthsFromToday = LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE)
    val blockingArchiveDates = mutableListOf<LocalDate>()

    val overlapBookings = cas3v2BookingRepository.findActiveOverlappingBookingByBedspace(bedspaceId, LocalDate.now(clock))

    overlapBookings.map {
      val bookingTurnaround = workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0)
      if (bookingTurnaround >= threeMonthsFromToday) {
        blockingArchiveDates += bookingTurnaround
      }
    }

    val overlappingVoid = cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateV2(
      bedspaceId,
      threeMonthsFromToday,
    ).maxByOrNull { it.endDate }

    if (overlappingVoid != null) {
      blockingArchiveDates += overlappingVoid.endDate
    }

    return if (blockingArchiveDates.isEmpty()) {
      CasResult.Success(null)
    } else {
      CasResult.Success(Cas3ValidationResult(bedspaceId, bedspacesEntity.reference, blockingArchiveDates.max()))
    }
  }

  private fun unarchivePremisesAndSaveDomainEvent(premises: Cas3PremisesEntity, restartDate: LocalDate, transactionId: UUID): Cas3PremisesEntity {
    val currentStartDate = premises.startDate
    val currentEndDate = premises.endDate
    premises.startDate = restartDate
    premises.endDate = null
    premises.status = Cas3PremisesStatus.online
    val updatedPremises = cas3PremisesRepository.save(premises)
    cas3v2DomainEventService.savePremisesUnarchiveEvent(premises, currentStartDate, restartDate, currentEndDate, transactionId)
    return updatedPremises
  }

  private fun unarchiveBedspaceAndSaveDomainEvent(bedspace: Cas3BedspacesEntity, premisesId: UUID, restartDate: LocalDate, transactionId: UUID): Cas3BedspacesEntity {
    val originalStartDate = bedspace.startDate
    val originalEndDate = bedspace.endDate!!

    val updatedBedspace = cas3BedspacesRepository.save(
      bedspace.copy(
        startDate = restartDate,
        endDate = null,
      ),
    )

    cas3v2DomainEventService.saveBedspaceUnarchiveEvent(updatedBedspace, premisesId, originalStartDate, originalEndDate, transactionId)
    return updatedBedspace
  }

  private fun canArchiveBedspace(bedspaceId: UUID, endDate: LocalDate) = canArchiveBedspace(filterByPremisesId = null, filterByBedspaceId = bedspaceId, endDate = endDate)

  private fun canArchivePremisesBedspaces(premisesId: UUID, endDate: LocalDate) = canArchiveBedspace(filterByPremisesId = premisesId, filterByBedspaceId = null, endDate = endDate)

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun canArchiveBedspace(filterByPremisesId: UUID?, filterByBedspaceId: UUID?, endDate: LocalDate): Cas3FieldValidationError<Cas3BedspacesEntity>? {
    val allBookings = when {
      filterByPremisesId != null -> cas3v2BookingRepository.findActiveOverlappingBookingByPremisesId(filterByPremisesId, LocalDate.now(clock)).sortedByDescending { it.departureDate }
      filterByBedspaceId != null -> cas3v2BookingRepository.findActiveOverlappingBookingByBedspace(filterByBedspaceId, LocalDate.now(clock)).sortedByDescending { it.departureDate }
      else -> emptyList()
    }
    val overlapBookings = allBookings.filter { it.departureDate > endDate }
    val lastOverlapVoid = when {
      filterByPremisesId != null -> cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateByPremisesId(filterByPremisesId, endDate).maxByOrNull { it.endDate }
      filterByBedspaceId != null -> cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateV2(filterByBedspaceId, endDate).maxByOrNull { it.endDate }
      else -> null
    }

    val lastBookingsTurnaroundDate = allBookings
      .asSequence()
      .mapNotNull { booking ->
        val turnaroundDate = workingDayService.addWorkingDays(booking.departureDate, booking.turnaround?.workingDayCount ?: 0)
        if (turnaroundDate > endDate) {
          booking.id to turnaroundDate
        } else {
          null
        }
      }
      .toMap()

    val (lastTurnaroundBookingId, lastTurnaroundDate) = lastBookingsTurnaroundDate.entries.maxByOrNull { it.value }?.let { it.key to it.value } ?: Pair(null, null)

    return when {
      isVoidLastOverlapBedspaceArchiveDate(lastOverlapVoid, lastTurnaroundDate) -> {
        Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = lastOverlapVoid?.bedspace?.id.toString(),
              message = "existingVoid",
              value = lastOverlapVoid?.endDate?.plusDays(1).toString(),
            ),
          ),
        )
      }
      lastTurnaroundDate != null -> {
        val lastOverlapBooking = getLastBookingOverlapBedspaceArchiveDate(overlapBookings, lastBookingsTurnaroundDate, lastTurnaroundDate)
        if (lastOverlapBooking != null && lastOverlapBooking.departureDate == lastTurnaroundDate) {
          Cas3FieldValidationError(
            mapOf(
              "$.endDate" to Cas3ValidationMessage(
                entityId = lastOverlapBooking.bedspace.id.toString(),
                message = "existingBookings",
                value = lastOverlapBooking.departureDate.plusDays(1).toString(),
              ),
            ),
          )
        } else {
          Cas3FieldValidationError(
            mapOf(
              "$.endDate" to Cas3ValidationMessage(
                entityId = allBookings.firstOrNull { it.id == lastTurnaroundBookingId }?.bedspace?.id.toString(),
                message = "existingTurnaround",
                value = lastTurnaroundDate.plusDays(1).toString(),
              ),
            ),
          )
        }
      }
      else -> null
    }
  }

  private fun isVoidLastOverlapBedspaceArchiveDate(
    lastOverlapVoid: Cas3VoidBedspaceEntity?,
    lastTurnaroundDate: LocalDate?,
  ) = lastOverlapVoid != null && (lastTurnaroundDate == null || (lastTurnaroundDate < lastOverlapVoid.endDate))

  private fun getLastBookingOverlapBedspaceArchiveDate(
    overlapBookings: List<Cas3BookingEntity>,
    lastBookingsTurnaroundDate: Map<UUID, LocalDate>,
    lastTurnaroundDate: LocalDate?,
  ): Cas3BookingEntity? {
    val lastOverlapBookingId = lastBookingsTurnaroundDate.entries.firstOrNull { it.value == lastTurnaroundDate }?.key
    return overlapBookings.firstOrNull { it.id == lastOverlapBookingId }
  }

  private fun archiveBedspaceAndSaveDomainEvent(bedspace: Cas3BedspacesEntity, premisesId: UUID, endDate: LocalDate, transactionId: UUID): Cas3BedspacesEntity {
    val originalEndDate = bedspace.endDate
    bedspace.endDate = endDate
    val updatedBedspace = cas3BedspacesRepository.save(bedspace)
    cas3v2DomainEventService.saveBedspaceArchiveEvent(updatedBedspace, premisesId, originalEndDate, transactionId)
    return updatedBedspace
  }

  private fun archivePremisesIfAllBedspacesArchived(premises: Cas3PremisesEntity, transactionId: UUID) {
    val bedspaces = cas3BedspacesRepository.findByPremisesId(premises.id)
    val lastBedspaceEndDate = bedspaces
      .asSequence()
      .mapNotNull { it.endDate }
      .maxOrNull()
    if (lastBedspaceEndDate != null && bedspaces.all { it.endDate != null }) {
      archivePremisesAndSaveDomainEvent(premises, lastBedspaceEndDate, transactionId)
    }
  }

  private fun archivePremisesAndSaveDomainEvent(premises: Cas3PremisesEntity, endDate: LocalDate, transactionId: UUID): Cas3PremisesEntity {
    premises.endDate = endDate
    premises.status = Cas3PremisesStatus.archived
    val updatedPremises = cas3PremisesRepository.save(premises)
    cas3v2DomainEventService.savePremisesArchiveEvent(premises, endDate, transactionId)
    return updatedPremises
  }
}
