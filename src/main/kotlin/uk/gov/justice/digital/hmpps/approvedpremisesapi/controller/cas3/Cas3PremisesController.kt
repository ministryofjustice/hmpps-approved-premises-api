package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.PremisesCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.TemporaryAccommodationPremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas3PremisesController(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val cas3BookingService: Cas3BookingService,
  private val temporaryAccommodationPremisesService: TemporaryAccommodationPremisesService,
  private val cas3PremisesService: Cas3PremisesService,
  private val premisesTransformer: PremisesTransformer,
  private val cas3PremisesTransformer: Cas3PremisesTransformer,
  private val cas3FutureBookingTransformer: Cas3FutureBookingTransformer,
  private val cas3PremisesSummaryTransformer: Cas3PremisesSummaryTransformer,
  private val cas3DepartureTransformer: Cas3DepartureTransformer,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
) : PremisesCas3Delegate {

  override fun getPremisesBedspaces(premisesId: UUID): ResponseEntity<List<Cas3Bedspace>> {
    val premises = temporaryAccommodationPremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premises.rooms.mapNotNull(cas3BedspaceTransformer::transformJpaToApi))
  }

  override fun getPremisesBedspace(premisesId: UUID, bedspaceId: UUID): ResponseEntity<Cas3Bedspace> {
    val premises = temporaryAccommodationPremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val bedspace = premises.rooms.flatMap { it.beds }.firstOrNull { it.id == bedspaceId } ?: throw NotFoundProblem(bedspaceId, "Bedspace")

    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(bedspace))
  }

  fun premisesTemporaryAccommodationPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = temporaryAccommodationPremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }
    val totalBeds = temporaryAccommodationPremisesService.getBedCount(premises)
    val availableBedsForToday =
      temporaryAccommodationPremisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
        .values.first().getFreeCapacity(totalBeds)
    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises, totalBeds, availableBedsForToday))
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }
    val totalBeds = cas3PremisesService.getBedspaceCount(premises)
    val availableBedsForToday = 0 // todo: replace with below equivalent
//      cas3PremisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
//        .values.first().getFreeCapacity(totalBeds)

    return ResponseEntity.ok(cas3PremisesTransformer.transformJpaToApi(premises, totalBeds, availableBedsForToday))
  }

  override fun getPremisesSummary(postcodeOrAddress: String?, propertyStatus: Cas3PropertyStatus?): ResponseEntity<List<Cas3PremisesSummary>> {
    val user = userService.getUserForRequest()
    val premisesSummariesByPremisesId = temporaryAccommodationPremisesService.getAllPremisesSummaries(user.probationRegion.id, postcodeOrAddress, propertyStatus).groupBy { it.id }
    val transformedSummaries = premisesSummariesByPremisesId.map { map ->
      cas3PremisesSummaryTransformer.transformDomainToCas3PremisesSummary(
        map.value.first(),
        map.value.filter { it.bedspaceId != null }.map(cas3PremisesSummaryTransformer::transformDomainToCas3BedspaceSummary),
      )
    }

    return ResponseEntity.ok(transformedSummaries.sortedBy { it.id })
  }

  override fun postPremisesBookingDeparture(
    premisesId: UUID,
    bookingId: UUID,
    body: Cas3NewDeparture,
  ): ResponseEntity<Cas3Departure> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = userService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = cas3BookingService.createDeparture(
      user = user,
      booking = booking,
      dateTime = body.dateTime.atOffset(ZoneOffset.UTC),
      reasonId = body.reasonId,
      moveOnCategoryId = body.moveOnCategoryId,
      notes = body.notes,
    )

    val departure = extractEntityFromCasResult(result)

    return ResponseEntity.ok(cas3DepartureTransformer.transformJpaToApi(departure))
  }

  override fun getPremisesFutureBookings(
    premisesId: UUID,
    statuses: List<BookingStatus>,
  ): ResponseEntity<List<FutureBooking>> {
    val user = userService.getUserForRequest()

    val result = cas3BookingService.findFutureBookingsForPremises(premisesId, statuses, user)

    val futureBookings = extractEntityFromCasResult(result)

    return ResponseEntity.ok(
      futureBookings.map { fb -> cas3FutureBookingTransformer.transformJpaToApi(fb.booking, fb.personInfo) },
    )
  }

  private fun getBookingForPremisesOrThrow(premisesId: UUID, bookingId: UUID): BookingEntity {
    val premises = temporaryAccommodationPremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return when (val result = cas3BookingService.getBookingForPremises(premises, bookingId)) {
      is GetBookingForPremisesResult.Success -> result.booking
      is GetBookingForPremisesResult.BookingNotFound -> throw NotFoundProblem(bookingId, "Booking")
    }
  }
}
