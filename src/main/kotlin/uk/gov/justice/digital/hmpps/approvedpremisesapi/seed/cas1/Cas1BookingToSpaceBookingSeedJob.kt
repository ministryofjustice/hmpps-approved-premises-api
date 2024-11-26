package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ManagementInfoSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ReferralDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeliusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.lang.Thread.sleep
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
class Cas1BookingToSpaceBookingSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val bookingRepository: BookingRepository,
  private val domainEventRepository: DomainEventRepository,
  private val domainEventService: DomainEventService,
  private val userRepository: UserRepository,
  private val deliusService: DeliusService,
  private val transactionTemplate: TransactionTemplate,
) : SeedJob<Cas1BookingToSpaceBookingSeedCsvRow>(
  id = UUID.randomUUID(),
  requiredHeaders = setOf(
    "premises_id",
  ),
  runInTransaction = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1BookingToSpaceBookingSeedCsvRow(
    premisesId = UUID.fromString(columns["premises_id"]!!.trim()),
  )

  override fun processRow(row: Cas1BookingToSpaceBookingSeedCsvRow) {
    transactionTemplate.executeWithoutResult {
      migratePremise(row.premisesId)
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun migratePremise(premisesId: UUID) {
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

    spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = booking.id,
        premises = premises,
        application = application,
        offlineApplication = offlineApplication,
        placementRequest = booking.placementRequest,
        createdBy = bookingMadeDomainEvent?.let { getCreatedByUser(it) },
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
        keyWorkerStaffCode = null,
        keyWorkerName = null,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = booking.cancellation?.date,
        cancellationRecordedAt = booking.cancellation?.createdAt?.toInstant(),
        cancellationReason = booking.cancellation?.reason,
        cancellationReasonNotes = booking.cancellation?.otherReason,
        departureMoveOnCategory = managementInfo.departureMoveOnCategory,
        departureReason = managementInfo.departureReason,
        departureNotes = managementInfo.departureNotes,
        criteria = booking.getEssentialRoomCriteria(),
        nonArrivalReason = managementInfo.nonArrivalReason,
        nonArrivalConfirmedAt = managementInfo.nonArrivalConfirmedAt?.toInstant(),
        nonArrivalNotes = managementInfo.nonArrivalNotes,
        migratedFromBooking = booking,
        deliusEventNumber = bookingMadeDomainEvent?.let { getDomainEventNumber(bookingMadeDomainEvent) },
        migratedManagementInfoFrom = managementInfo.source.entityEquivalent,
      ),
    )

    log.info("Have migrated booking $bookingId to space booking")
  }

  @SuppressWarnings("MagicNumber")
  private fun getManagementInfo(booking: BookingEntity): ManagementInfo {
    val shouldExistInDelius = !booking.isCancelled
    val referralDetails = if (shouldExistInDelius) {
      sleep(Duration.ofMillis(20))
      deliusService.getReferralDetails(booking)
    } else {
      null
    }

    if (shouldExistInDelius && referralDetails == null) {
      log.warn("Could not retrieve referral details from delius for booking ${booking.id}")
    }

    return referralDetails?.toManagementInfo() ?: booking.toManagementInfo()
  }

  private fun ReferralDetail.toManagementInfo() = ManagementInfo(
    source = SeedManagementInfoSource.Delius,
    arrivedAtDate = arrivedAt?.toLocalDate(),
    arrivedAtTime = arrivedAt?.toLocalTime(),
    departedAtDate = departedAt?.toLocalDate(),
    departedAtTime = departedAt?.toLocalTime(),
    keyWorkerStaffCode = null,
    keyWorkerName = null,
    departureReason = null,
    departureMoveOnCategory = null,
    departureNotes = null,
    nonArrivalConfirmedAt = null,
    nonArrivalReason = null,
    nonArrivalNotes = null,
  )

  private fun BookingEntity.toManagementInfo() = ManagementInfo(
    source = SeedManagementInfoSource.LegacyCas1,
    arrivedAtDate = arrival?.arrivalDateTime?.toLocalDate(),
    arrivedAtTime = arrival?.arrivalDateTime?.toLocalDateTime()?.toLocalTime(),
    departedAtDate = departure?.dateTime?.toLocalDate(),
    departedAtTime = departure?.dateTime?.toLocalDateTime()?.toLocalTime(),
    keyWorkerStaffCode = null,
    keyWorkerName = null,
    departureReason = departure?.reason,
    departureMoveOnCategory = departure?.moveOnCategory,
    departureNotes = departure?.notes,
    nonArrivalConfirmedAt = nonArrival?.createdAt,
    nonArrivalReason = nonArrival?.reason,
    nonArrivalNotes = nonArrival?.notes,
  )

  private data class ManagementInfo(
    val source: SeedManagementInfoSource,
    val arrivedAtDate: LocalDate?,
    val arrivedAtTime: LocalTime?,
    val departedAtDate: LocalDate?,
    val departedAtTime: LocalTime?,
    val keyWorkerStaffCode: String?,
    val keyWorkerName: String?,
    val departureReason: DepartureReasonEntity?,
    val departureMoveOnCategory: MoveOnCategoryEntity?,
    val departureNotes: String?,
    val nonArrivalConfirmedAt: OffsetDateTime?,
    val nonArrivalReason: NonArrivalReasonEntity?,
    val nonArrivalNotes: String?,
  )

  private enum class SeedManagementInfoSource(
    val entityEquivalent: ManagementInfoSource,
  ) {
    Delius(ManagementInfoSource.DELIUS),
    LegacyCas1(ManagementInfoSource.LEGACY_CAS_1),
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
