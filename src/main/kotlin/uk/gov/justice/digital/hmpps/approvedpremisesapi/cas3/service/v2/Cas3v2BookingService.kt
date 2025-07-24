package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingAndPersons
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.getPersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2BookingService(
  private val cas3BookingRepository: Cas3v2BookingRepository,
  private val cas3BedspaceRepository: Cas3BedspacesRepository,
  private val cas3ArrivalRepository: Cas3ArrivalRepository,
  private val cas3v2TurnaroundRepository: Cas3v2TurnaroundRepository,
  private val assessmentRepository: AssessmentRepository,
  private val offenderService: OffenderService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val workingDayService: WorkingDayService,
  private val userAccessService: UserAccessService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getBooking(bookingId: UUID, premisesId: UUID?): CasResult<Cas3BookingAndPersons> {
    val booking = cas3BookingRepository.findByIdOrNull(bookingId)
      ?: return CasResult.NotFound("Booking", bookingId.toString())

    val user = userService.getUserForRequest()
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      return CasResult.Unauthorised()
    } else if (premisesId != null && premisesId != booking.premises.id) {
      return CasResult.GeneralValidationError("The supplied premisesId does not match the booking's premises")
    }

    val personInfo = offenderDetailService.getPersonInfoResult(
      booking.crn,
      user.deliusUsername,
      user.hasQualification(
        UserQualification.LAO,
      ),
    )

    return CasResult.Success(Cas3BookingAndPersons(booking, personInfo))
  }

  @Transactional
  fun createBooking(
    user: UserEntity,
    premises: Cas3PremisesEntity,
    crn: String,
    nomsNumber: String?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedspaceId: UUID?,
    assessmentId: UUID?,
  ): CasResult<Cas3BookingEntity> = validatedCasResult {
    if (bedspaceId == null) {
      "$.bedspaceId" hasValidationError "empty"
      return@validatedCasResult fieldValidationError
    }

    getBookingWithConflictingDates(arrivalDate, departureDate, bookingId = null, bedspaceId)?.let {
      return@validatedCasResult it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate()} which overlaps with the desired dates"
    }

    cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
      bedspaceId,
      startDate = arrivalDate,
      endDate = departureDate,
    ).firstOrNull()?.let {
      return@validatedCasResult it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    cas3BedspaceRepository.findArchivedBedspaceByBedspaceIdAndDate(bedspaceId, departureDate)?.let {
      return@validatedCasResult it.id hasConflictError "BedSpace is archived from ${it.endDate} which overlaps with the desired dates"
    }

    val bedspace = cas3BedspaceRepository.findByIdOrNull(bedspaceId)
    if (bedspace == null) {
      "$.bedId" hasValidationError "doesNotExist"
    } else if (bedspace.startDate != null && bedspace.startDate!!.isAfter(arrivalDate)) {
      "$.arrivalDate" hasValidationError "bookingArrivalDateBeforeBedspaceStartDate"
    }

    if (departureDate.isBefore(arrivalDate)) {
      "$.departureDate" hasValidationError "beforeBookingArrivalDate"
    }

    val application = assessmentId?.let { id ->
      val result = assessmentRepository.findByIdOrNull(id)
      if (result == null) {
        "$.assessmentId" hasValidationError "doesNotExist"
      }
      result?.application
    }

    if (validationErrors.any()) {
      return@validatedCasResult fieldValidationError
    }

    val personResult = offenderService.getPersonSummaryInfoResult(crn, LaoStrategy.NeverRestricted)
    val offenderName = personResult.getPersonName()

    if (offenderName == null) {
      log.warn("Unable to get offender name for CRN $crn")
    }

    val bookingCreatedAt = OffsetDateTime.now()

    val booking = cas3BookingRepository.save(
      Cas3BookingEntity(
        id = UUID.randomUUID(),
        crn = crn,
        nomsNumber = nomsNumber,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
        arrivals = mutableListOf(),
        departures = mutableListOf(),
        nonArrival = null,
        cancellations = mutableListOf(),
        confirmation = null,
        extensions = mutableListOf(),
        dateChanges = mutableListOf(),
        premises = premises,
        bedspace = bedspace!!,
        service = ServiceName.temporaryAccommodation.value,
        originalArrivalDate = arrivalDate,
        originalDepartureDate = departureDate,
        createdAt = bookingCreatedAt,
        application = application,
        turnarounds = mutableListOf(),
        status = Cas3BookingStatus.provisional,
        offenderName = offenderName,
      ),
    )

    val turnaround = cas3v2TurnaroundRepository.save(
      Cas3v2TurnaroundEntity(
        id = UUID.randomUUID(),
        workingDayCount = 0,
        createdAt = bookingCreatedAt,
        booking = booking,
      ),
    )

    booking.turnarounds += turnaround

    cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(booking, user)

    success(booking)
  }

  @Transactional
  fun createArrival(
    user: UserEntity,
    booking: Cas3BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
  ): CasResult<Cas3ArrivalEntity> = validatedCasResult {
    if (expectedDepartureDate.isBefore(arrivalDate)) {
      "$.expectedDepartureDate" hasValidationError "beforeBookingArrivalDate"
      return@validatedCasResult fieldValidationError
    }
    val isFirstArrival = booking.arrivals.isEmpty()
    val arrivalEntity = cas3ArrivalRepository.save(
      Cas3ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalDateTime = arrivalDate.toLocalDateTime().toInstant(),
        expectedDepartureDate = expectedDepartureDate,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )

    booking.arrivalDate = arrivalDate
    booking.departureDate = expectedDepartureDate
    booking.status = Cas3BookingStatus.arrived
    updateBooking(booking)

    booking.arrivals += arrivalEntity

    when (isFirstArrival) {
      true -> cas3DomainEventService.savePersonArrivedEvent(booking, user)
      else -> cas3DomainEventService.savePersonArrivedUpdatedEvent(booking, user)
    }

    success(arrivalEntity)
  }

  private fun updateBooking(bookingEntity: Cas3BookingEntity) = cas3BookingRepository.save(bookingEntity)

  fun getBookingWithConflictingDates(
    arrivalDate: LocalDate,
    closedDate: LocalDate,
    bookingId: UUID?,
    bedspaceId: UUID,
  ): Cas3BookingEntity? = cas3BookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspaceId, closedDate, excludeBookingId = bookingId)
    .firstOrNull { it.lastUnavailableDate() >= arrivalDate }

  fun getVoidBedspaceWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    bookingId: UUID?,
    bedspaceId: UUID,
  ) = cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
    bedspaceId,
    startDate,
    endDate,
    bookingId,
  ).firstOrNull()

  fun Cas3BookingEntity.lastUnavailableDate() = workingDayService.addWorkingDays(this.departureDate, this.turnaround?.workingDayCount ?: 0)
}
