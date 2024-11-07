package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeliusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SuppressWarnings("LongParameterList")
class Cas1BookingToSpaceBookingSeedJob(
  fileName: String,
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val bookingRepository: BookingRepository,
  private val domainEventRepository: DomainEventRepository,
  private val domainEventService: DomainEventService,
  private val userRepository: UserRepository,
  private val deliusService: DeliusService,
) : SeedJob<Cas1BookingToSpaceBookingSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "premises_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1BookingToSpaceBookingSeedCsvRow(
    premisesId = UUID.fromString(columns["premises_id"]!!.trim()),
  )

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun processRow(row: Cas1BookingToSpaceBookingSeedCsvRow) {
    val premisesId = row.premisesId

    val premises = approvedPremisesRepository.findByIdOrNull(premisesId) ?: error("Premises with id $premisesId not found")

    log.info("Deleting all existing migrated space bookings for premises ${premises.name}")
    val deletedCount = spaceBookingRepository.deleteByPremisesIdAndMigratedFromBookingIsNotNull(premisesId)
    log.info("Have deleted $deletedCount existing migrated space bookings")

    val bookingIds = bookingRepository.findAllIdsByPremisesId(premisesId)
    val bookingsToMigrateSize = bookingIds.size
    log.info("Have found $bookingsToMigrateSize bookings for premise ${premises.name}")

    var migratedCount = 0
    bookingIds.forEach { bookingId ->
      try {
        migrateBooking(premises, bookingId)
        migratedCount++
      } catch (e: Throwable) {
        log.error("Error migrating booking $bookingId", e)
      }
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

    val spaceBooking = spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = premises,
        application = application,
        offlineApplication = offlineApplication,
        placementRequest = booking.placementRequest,
        createdBy = bookingMadeDomainEvent?.let { getCreatedByUser(it) },
        createdAt = booking.createdAt,
        expectedArrivalDate = booking.arrivalDate,
        expectedDepartureDate = booking.departureDate,
        actualArrivalDateTime = managementInfo?.arrivedAt,
        actualDepartureDateTime = managementInfo?.departedAt,
        canonicalArrivalDate = managementInfo?.arrivedAt?.toLocalDate() ?: booking.arrivalDate,
        canonicalDepartureDate = managementInfo?.departedAt?.toLocalDate() ?: booking.departureDate,
        crn = booking.crn,
        keyWorkerStaffCode = null,
        keyWorkerName = null,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = booking.cancellation?.date,
        cancellationRecordedAt = booking.cancellation?.createdAt?.toInstant(),
        cancellationReason = booking.cancellation?.reason,
        cancellationReasonNotes = booking.cancellation?.otherReason,
        departureMoveOnCategory = null,
        departureReason = null,
        criteria = booking.getEssentialRoomCriteria(),
        nonArrivalReason = null,
        nonArrivalConfirmedAt = null,
        nonArrivalNotes = null,
        migratedFromBooking = booking,
        deliusEventNumber = bookingMadeDomainEvent?.let { getDomainEventNumber(bookingMadeDomainEvent) },
      ),
    )

    log.info("Have migrated booking $bookingId to space booking ${spaceBooking.id}")
  }

  @SuppressWarnings("MagicNumber")
  private fun getManagementInfo(booking: BookingEntity): ManagementInfo? {
    val shouldExistInDelius = !booking.isCancelled
    val referralDetails = if (shouldExistInDelius) {
      sleep(Duration.ofMillis(50))
      deliusService.getReferralDetails(booking)
    } else {
      null
    }

    if (shouldExistInDelius && referralDetails == null) {
      log.warn("Could not retrieve referral details from delius for booking ${booking.id}")
    }

    if (referralDetails != null) {
      return ManagementInfo(
        source = ManagementInfoSource.Delius,
        arrivedAt = referralDetails.arrivedAt?.toInstant(),
        departedAt = referralDetails.departedAt?.toInstant(),
      )
    }

    if (booking.hasArrivals()) {
      return ManagementInfo(
        source = ManagementInfoSource.LegacyCas1,
        arrivedAt = booking.arrival?.arrivalDateTime,
        departedAt = booking.departure?.dateTime?.toInstant(),
      )
    }

    return null
  }

  data class ManagementInfo(
    val source: ManagementInfoSource,
    val arrivedAt: Instant?,
    val departedAt: Instant?,
  )

  enum class ManagementInfoSource {
    Delius,
    LegacyCas1,
  }

  private fun BookingEntity.getEssentialRoomCriteria() =
    placementRequest?.placementRequirements?.essentialCriteria?.filter { it.isModelScopeRoom() }?.toList()
      ?: emptyList()

  private fun getCreatedByUser(bookingMadeDomainEvent: DomainEvent<BookingMadeEnvelope>): UserEntity {
    val createdByUsernameUpper =
      bookingMadeDomainEvent.data.eventDetails.bookedBy.staffMember!!.username?.uppercase()
        ?: error("Can't find created by username for booking ${bookingMadeDomainEvent.bookingId}")
    return userRepository.findByDeliusUsername(createdByUsernameUpper) ?: error("Can't find user with username $createdByUsernameUpper")
  }

  private fun getDomainEventNumber(bookingMadeDomainEvent: DomainEvent<BookingMadeEnvelope>): String {
    return bookingMadeDomainEvent.data.eventDetails.deliusEventNumber
  }

  private fun getBookingMadeDomainEvent(bookingId: UUID): DomainEvent<BookingMadeEnvelope>? {
    val createdDomainEvents = domainEventRepository.findIdsByTypeAndBookingId(DomainEventType.APPROVED_PREMISES_BOOKING_MADE, bookingId)
    if (createdDomainEvents.isEmpty()) {
      return null
    }

    return domainEventService.getBookingMadeEvent(createdDomainEvents.first())
  }
}

data class Cas1BookingToSpaceBookingSeedCsvRow(
  val premisesId: UUID,
)
