package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.PremisesCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSortBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas3PremisesController(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val cas3BookingService: Cas3BookingService,
  private val cas3PremisesService: Cas3PremisesService,
  private val cas3FutureBookingTransformer: Cas3FutureBookingTransformer,
  private val cas3PremisesSummaryTransformer: Cas3PremisesSummaryTransformer,
  private val cas3PremisesSearchResultsTransformer: Cas3PremisesSearchResultsTransformer,
  private val cas3DepartureTransformer: Cas3DepartureTransformer,
  private val cas3PremisesTransformer: Cas3PremisesTransformer,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
) : PremisesCas3Delegate {
  override fun getPremisesById(premisesId: UUID): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(cas3PremisesTransformer.transformDomainToApi(premises))
  }

  override fun getPremisesBedspaces(premisesId: UUID): ResponseEntity<Cas3Bedspaces> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val bedspaces = premises.rooms.flatMap { it.beds }

    val result = Cas3Bedspaces(
      bedspaces = bedspaces.map(cas3BedspaceTransformer::transformJpaToApi),
      totalOnlineBedspaces = bedspaces.count { it.isCas3BedspaceOnline() },
      totalUpcomingBedspaces = bedspaces.count { it.isCas3BedspaceUpcoming() },
      totalArchivedBedspaces = bedspaces.count { it.isCas3BedspaceArchived() },
    )

    return ResponseEntity.ok(result)
  }

  override fun getPremisesBedspace(premisesId: UUID, bedspaceId: UUID): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val bedspace = premises.rooms.flatMap { it.beds }.firstOrNull { it.id == bedspaceId } ?: throw NotFoundProblem(bedspaceId, "Bedspace")

    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(bedspace))
  }

  override fun createBedspace(premisesId: UUID, newBedspace: Cas3NewBedspace): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val bedspace = extractEntityFromCasResult(
      cas3PremisesService.createBedspace(premises, newBedspace.reference, newBedspace.startDate, newBedspace.notes, newBedspace.characteristicIds),
    )

    return ResponseEntity(cas3BedspaceTransformer.transformJpaToApi(bedspace), HttpStatus.CREATED)
  }

  @Transactional
  override fun updateBedspace(
    premisesId: UUID,
    bedspaceId: UUID,
    updateBedspace: Cas3UpdateBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val updatedBedspace = extractEntityFromCasResult(
      cas3PremisesService.updateBedspace(premises, bedspaceId, updateBedspace.reference, updateBedspace.notes, updateBedspace.characteristicIds),
    )

    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(updatedBedspace))
  }

  override fun getPremisesSummary(postcodeOrAddress: String?, sortBy: Cas3PremisesSortBy?): ResponseEntity<List<Cas3PremisesSummary>> {
    val user = userService.getUserForRequest()
    val premisesSummariesByPremisesId = cas3PremisesService.getAllPremisesSummaries(user.probationRegion.id, postcodeOrAddress, premisesStatus = null).groupBy { it.id }
    val transformedSummaries = premisesSummariesByPremisesId.map { map ->
      cas3PremisesSummaryTransformer.transformDomainToCas3PremisesSummary(
        map.value.first(),
        map.value.count { isBedspaceStatusOnlineOrUpcoming(it) },
      )
    }

    return ResponseEntity.ok(
      when (sortBy) {
        Cas3PremisesSortBy.pdu -> transformedSummaries.sortedBy { it.pdu.lowercase() }
        Cas3PremisesSortBy.la -> transformedSummaries.sortedBy { it.localAuthorityAreaName?.lowercase() }
        null -> transformedSummaries.sortedBy { it.id }
      },
    )
  }

  override fun searchPremises(postcodeOrAddress: String?, premisesStatus: Cas3PremisesStatus?): ResponseEntity<Cas3PremisesSearchResults> {
    val user = userService.getUserForRequest()
    val premisesById = cas3PremisesService.getAllPremisesSummaries(user.probationRegion.id, postcodeOrAddress, premisesStatus).groupBy { it.id }
    val premisesSearchResults = cas3PremisesSearchResultsTransformer.transformDomainToCas3PremisesSearchResults(premisesById)
    val sortedResults = Cas3PremisesSearchResults(
      results = premisesSearchResults.results?.sortedBy { it.id },
      totalPremises = premisesSearchResults.totalPremises,
      totalOnlineBedspaces = premisesSearchResults.totalOnlineBedspaces,
      totalUpcomingBedspaces = premisesSearchResults.totalUpcomingBedspaces,
    )

    return ResponseEntity.ok(sortedResults)
  }

  override fun postPremisesBookingDeparture(
    premisesId: UUID,
    bookingId: UUID,
    body: Cas3NewDeparture,
  ): ResponseEntity<Cas3Departure> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = userService.getUserForRequest()

    if (!userAccessService.userCanManageCas3PremisesBookings(user, booking.premises)) {
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
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return when (val result = cas3BookingService.getBookingForPremises(premises, bookingId)) {
      is GetBookingForPremisesResult.Success -> result.booking
      is GetBookingForPremisesResult.BookingNotFound -> throw NotFoundProblem(bookingId, "Booking")
    }
  }

  private fun isBedspaceStatusOnlineOrUpcoming(premisesSummary: TemporaryAccommodationPremisesSummary) = premisesSummary.bedspaceId != null &&
    (premisesSummary.bedspaceStatus == Cas3BedspaceStatus.online || premisesSummary.bedspaceStatus == Cas3BedspaceStatus.upcoming)
}
