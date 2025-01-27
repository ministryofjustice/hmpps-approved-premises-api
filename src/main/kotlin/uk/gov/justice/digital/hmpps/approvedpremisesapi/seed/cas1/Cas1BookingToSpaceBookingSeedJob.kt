package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Component
@SuppressWarnings("LongParameterList")
class Cas1BookingToSpaceBookingSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val bookingRepository: BookingRepository,
  private val domainEventRepository: DomainEventRepository,
  private val domainEventService: Cas1DomainEventService,
  private val userRepository: UserRepository,
  private val transactionTemplate: TransactionTemplate,
  private val cas1DeliusBookingImportRepository: Cas1DeliusBookingImportRepository,
  private val cas1BookingManagementInfoService: Cas1BookingManagementInfoService,
  private val environmentService: EnvironmentService,
  private val placementRequestRepository: PlacementRequestRepository,
) : SeedJob<Cas1BookingToSpaceBookingSeedCsvRow>(
  requiredHeaders = setOf(
    "q_code",
  ),
  runInTransaction = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1BookingToSpaceBookingSeedCsvRow(
    qCode = columns["q_code"]!!.trim(),
  )

  override fun preSeed() {
    if (environmentService.isProd()) {
      error("Cannot run seed job in prod")
    }
  }

  override fun processRow(row: Cas1BookingToSpaceBookingSeedCsvRow) {
    transactionTemplate.executeWithoutResult {
      migratePremise(row.qCode)
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun migratePremise(qCode: String) {
    val premises = approvedPremisesRepository.findByQCode(qCode) ?: error("Premises with qcode $qCode not found")

    if (!premises.supportsSpaceBookings) {
      error("premise ${premises.name} doesn't support space bookings, can't migrate bookings")
    }

    val bookingIds = bookingRepository.findAllIdsByPremisesId(premises.id)
    val bookingsToMigrateSize = bookingIds.size
    log.info("Have found $bookingsToMigrateSize bookings for premise ${premises.name}")

    var migratedCount = 0
    var failedCount = 0
    bookingIds.forEach { bookingId ->
      try {
        migrateBooking(premises, bookingId)
        migratedCount++
      } catch (e: Throwable) {
        log.error("Error migrating booking $bookingId", e)
        failedCount++
      }
    }

    if (failedCount > 0) {
      error("Could not migrate $failedCount of $bookingsToMigrateSize bookings for premise ${premises.name}. Transaction will be rolled back")
    }

    log.info("Have successfully migrated $migratedCount of $bookingsToMigrateSize bookings for premise ${premises.name}")
  }

  private fun migrateBooking(
    premises: ApprovedPremisesEntity,
    bookingId: UUID,
  ) {
    val booking = bookingRepository.findByIdOrNull(bookingId)!!

    val application = booking.application as ApprovedPremisesApplicationEntity?
    val offlineApplication = booking.offlineApplication
    val bookingMadeDomainEvent = getBookingMadeDomainEvent(bookingId)
    val managementInfo = getManagementInfo(booking)

    spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = booking.id,
        premises = premises,
        application = application,
        offlineApplication = offlineApplication,
        placementRequest = booking.placementRequest,
        createdBy = bookingMadeDomainEvent?.getCreatedByUser(),
        createdAt = booking.createdAt,
        expectedArrivalDate = booking.arrivalDate,
        expectedDepartureDate = booking.departureDate,
        actualArrivalDate = managementInfo.arrivedAtDate,
        actualArrivalTime = managementInfo.arrivedAtTime,
        actualDepartureDate = managementInfo.departedAtDate,
        actualDepartureTime = managementInfo.departedAtTime,
        canonicalArrivalDate = managementInfo.arrivedAtDate ?: booking.arrivalDate,
        canonicalDepartureDate = managementInfo.departedAtDate ?: booking.departureDate,
        crn = booking.crn,
        keyWorkerStaffCode = managementInfo.keyWorkerStaffCode,
        keyWorkerName = managementInfo.keyWorkerName,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = booking.cancellation?.date,
        cancellationRecordedAt = booking.cancellation?.createdAt?.toInstant(),
        cancellationReason = booking.cancellation?.reason,
        cancellationReasonNotes = booking.cancellation?.otherReason,
        departureMoveOnCategory = managementInfo.departureMoveOnCategory,
        departureReason = managementInfo.departureReason,
        departureNotes = managementInfo.departureNotes,
        criteria = booking.getEssentialCharacteristicsOfInterest().toMutableList(),
        nonArrivalReason = managementInfo.nonArrivalReason,
        nonArrivalConfirmedAt = managementInfo.nonArrivalConfirmedAt?.toInstant(),
        nonArrivalNotes = managementInfo.nonArrivalNotes,
        deliusEventNumber = bookingMadeDomainEvent?.data?.eventDetails?.deliusEventNumber,
        migratedManagementInfoFrom = managementInfo.source,
        deliusId = managementInfo.deliusId,
      ),
    )

    domainEventRepository.replaceBookingIdWithSpaceBookingId(bookingId)

    booking.placementRequest?.let {
      it.booking = null
      placementRequestRepository.save(it)
    }
    bookingRepository.delete(booking)

    log.info("Have migrated booking $bookingId to space booking")
  }

  @SuppressWarnings("MagicNumber")
  private fun getManagementInfo(booking: BookingEntity): ManagementInfo {
    val shouldExistInDelius = !booking.isCancelled
    val deliusImport = if (shouldExistInDelius) {
      cas1DeliusBookingImportRepository.findByBookingId(booking.id)
    } else {
      null
    }

    if (shouldExistInDelius && deliusImport == null) {
      log.warn("Could not retrieve referral details from delius import for booking ${booking.id}")
    }

    return if (deliusImport != null) {
      cas1BookingManagementInfoService.fromDeliusBookingImport(deliusImport)
    } else {
      cas1BookingManagementInfoService.fromBooking(booking)
    }
  }

  private fun BookingEntity.getEssentialCharacteristicsOfInterest() =
    placementRequest?.placementRequirements?.essentialCriteria
      ?.filter { Cas1SpaceBookingEntity.CHARACTERISTICS_OF_INTEREST.contains(it.propertyName) }
      ?.toList()
      ?: emptyList()

  private fun DomainEvent<BookingMadeEnvelope>.getCreatedByUser(): UserEntity {
    val createdByUsernameUpper =
      data.eventDetails.bookedBy.staffMember!!.username?.uppercase()
        ?: error("Can't find created by username for booking $bookingId")
    return userRepository.findByDeliusUsername(createdByUsernameUpper) ?: error("Can't find user with username $createdByUsernameUpper")
  }

  private fun getBookingMadeDomainEvent(bookingId: UUID) = domainEventRepository
    .findIdsByTypeAndBookingId(DomainEventType.APPROVED_PREMISES_BOOKING_MADE, bookingId)
    .firstOrNull()
    ?.let {
      domainEventService.getBookingMadeEvent(it)
    }
}

data class Cas1BookingToSpaceBookingSeedCsvRow(
  val qCode: String,
)
