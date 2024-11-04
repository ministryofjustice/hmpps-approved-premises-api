package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
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
    log.info("Have found ${bookingIds.size} bookings for premise ${premises.name}")

    bookingIds.forEach { bookingId ->
      try {
        migrateBooking(premises, bookingId)
      } catch (e: Throwable) {
        log.error("Error migrating booking $bookingId", e)
      }
    }
  }

  private fun migrateBooking(
    premises: ApprovedPremisesEntity,
    bookingId: UUID,
  ) {
    val booking = bookingRepository.findByIdOrNull(bookingId)!!

    if (booking.adhoc == true) {
      error("Can't currently migrate adhoc booking $bookingId")
    }

    if (booking.placementRequest == null) {
      error("Can't currently migrate booking $bookingId as it doesn't have a placement request")
    }

    if (booking.application == null) {
      error("Can't currently migrate booking $bookingId as it isn't linked to an application")
    }

    val application = booking.application!! as ApprovedPremisesApplicationEntity
    val bookingMadeDomainEvent = getBookingMadeDomainEvent(bookingId)
      ?: error("Can't find booking made domain event for booking $bookingId")

    val spaceBooking = spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = premises,
        application = application,
        placementRequest = booking.placementRequest!!,
        createdBy = getCreatedByUser(bookingMadeDomainEvent),
        createdAt = booking.createdAt,
        expectedArrivalDate = booking.arrivalDate,
        expectedDepartureDate = booking.departureDate,
        actualArrivalDateTime = null,
        actualDepartureDateTime = null,
        canonicalArrivalDate = booking.arrivalDate,
        canonicalDepartureDate = booking.departureDate,
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
        criteria = booking.placementRequest!!.placementRequirements.essentialCriteria.toList(),
        nonArrivalReason = null,
        nonArrivalConfirmedAt = null,
        nonArrivalNotes = null,
        migratedFromBooking = booking,
        deliusEventNumber = getDomainEventNumber(bookingMadeDomainEvent),
      ),
    )

    log.info("Have migrated booking $bookingId to space booking ${spaceBooking.id}")
  }

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
      error("Can't find a booking made domain event for booking $bookingId")
    }

    return domainEventService.getBookingMadeEvent(createdDomainEvents.first())
  }
}

data class Cas1BookingToSpaceBookingSeedCsvRow(
  val premisesId: UUID,
)
