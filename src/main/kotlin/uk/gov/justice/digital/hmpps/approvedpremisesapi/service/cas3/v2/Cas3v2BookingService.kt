package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.getPersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2BookingService(
  private val cas3BookingRepository: Cas3v2BookingRepository,
  private val cas3BedspaceRepository: Cas3BedspacesRepository,
  private val cas3TurnaroundRepository: Cas3v2TurnaroundRepository,
  private val assessmentRepository: AssessmentRepository,
  private val offenderService: OffenderService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

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
    enableTurnarounds: Boolean,
  ): AuthorisableActionResult<ValidatableActionResult<Cas3BookingEntity>> {
    val validationResult = validated {
      if (bedspaceId == null) {
        "$.bedspaceId" hasValidationError "empty"
        return@validated fieldValidationError
      }

      //TODO: in Cas3BookingService, we generate expectedLastUnavailableDate using premises.turnaroundWorkingDayCount. We will be tackling the new way of doing this as part of epic CAS-1655
      val expectedLastUnavailableDate = departureDate
      cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
        bedspaceId,
        startDate = arrivalDate,
        endDate = expectedLastUnavailableDate,
      ).firstOrNull()?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }

      cas3BedspaceRepository.findArchivedBedspaceByBedIdAndDate(bedspaceId, departureDate)?.let {
        return@validated it.id hasConflictError "Bedspace is archived from ${it.endDate} which overlaps with the desired dates"
      }

      val bedspace = cas3BedspaceRepository.findByIdOrNull(bedspaceId)
      if (bedspace == null) {
        "$.bedId" hasValidationError "doesNotExist"
      }

      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      val application = when (assessmentId) {
        null -> null
        else -> {
          val result = assessmentRepository.findByIdOrNull(assessmentId)
          if (result == null) {
            "$.assessmentId" hasValidationError "doesNotExist"
          }
          result?.application
        }
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
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
          status = BookingStatus.provisional,
          offenderName = offenderName,
        ),
      )

      val turnaround = cas3TurnaroundRepository.save(
        Cas3v2TurnaroundEntity(
          id = UUID.randomUUID(),
          // TODO: in Cas3BookingService, we determine this based on premises.turnaroundWorkingDayCount when enableTurnarounds == true. We will be tackling the new way of doing this as part of epic CAS-1655
          workingDayCount = 0,
          createdAt = bookingCreatedAt,
          booking = booking,
        ),
      )

      booking.turnarounds += turnaround

      cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(booking, user)

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }
}
