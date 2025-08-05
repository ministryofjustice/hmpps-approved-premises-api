package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesBedspaceTotals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSortBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.ZoneOffset
import java.util.UUID

@Suppress("LongParameterList", "TooManyFunctions")
@Cas3Controller
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
) {
  @PostMapping(
    "/premises",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun createPremises(@RequestBody body: Cas3NewPremises): ResponseEntity<Cas3Premises> {
    if (!userAccessService.currentUserCanAccessRegion(ServiceName.temporaryAccommodation, body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val premises = extractEntityFromCasResult(
      cas3PremisesService.createNewPremises(
        reference = body.reference,
        addressLine1 = body.addressLine1,
        addressLine2 = body.addressLine2,
        town = body.town,
        postcode = body.postcode,
        localAuthorityAreaId = body.localAuthorityAreaId,
        probationRegionId = body.probationRegionId,
        probationDeliveryUnitId = body.probationDeliveryUnitId,
        characteristicIds = body.characteristicIds,
        notes = body.notes,
        turnaroundWorkingDays = body.turnaroundWorkingDays,
      ),
    )

    return ResponseEntity(cas3PremisesTransformer.transformDomainToApi(premises), HttpStatus.CREATED)
  }

  @Transactional
  @PutMapping("/premises/{premisesId}")
  fun updatePremises(@PathVariable premisesId: UUID, @RequestBody body: Cas3UpdatePremises): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    return cas3PremisesService.updatePremises(
      premises = premises,
      addressLine1 = body.addressLine1,
      addressLine2 = body.addressLine2,
      town = body.town,
      postcode = body.postcode,
      localAuthorityAreaId = body.localAuthorityAreaId,
      probationRegionId = body.probationRegionId,
      characteristicIds = body.characteristicIds,
      notes = body.notes,
      probationDeliveryUnitId = body.probationDeliveryUnitId,
      turnaroundWorkingDays = body.turnaroundWorkingDayCount,
      reference = body.reference,
    )
      .let { extractEntityFromCasResult(it) }
      .let { cas3PremisesTransformer.transformDomainToApi(it) }
      .let { ResponseEntity.ok(it) }
  }

  @GetMapping("/premises/{premisesId}")
  fun getPremisesById(@PathVariable premisesId: UUID): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val archiveHistory = extractEntityFromCasResult(cas3PremisesService.getPremisesArchiveHistory(premises))

    return ResponseEntity.ok(cas3PremisesTransformer.transformDomainToApi(premises, archiveHistory))
  }

  @GetMapping("/premises/{premisesId}/bedspace-totals")
  fun getPremisesBedspaceTotals(@PathVariable premisesId: UUID): ResponseEntity<Cas3PremisesBedspaceTotals> {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val totalBedspaceByStatus = extractEntityFromCasResult(cas3PremisesService.getBedspaceTotals(premises))

    val result = Cas3PremisesBedspaceTotals(
      id = premises.id,
      status = if (premises.status == PropertyStatus.active) Cas3PremisesStatus.online else Cas3PremisesStatus.archived,
      totalOnlineBedspaces = totalBedspaceByStatus.onlineBedspaces,
      totalUpcomingBedspaces = totalBedspaceByStatus.upcomingBedspaces,
      totalArchivedBedspaces = totalBedspaceByStatus.archivedBedspaces,
    )

    return ResponseEntity.ok(result)
  }

  @GetMapping("/premises/{premisesId}/bedspaces")
  fun getPremisesBedspaces(@PathVariable premisesId: UUID): ResponseEntity<Cas3Bedspaces> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val bedspaces = cas3PremisesService.getPremisesBedspaces(premises.id)
    val bedspacesArchiveHistory = cas3PremisesService.getBedspacesArchiveHistory(bedspaces.map { it.id })

    val result = Cas3Bedspaces(
      bedspaces = bedspaces.map { bedspace ->
        val archiveHistory = bedspacesArchiveHistory
          .firstOrNull { bedspaceArchiveHistory -> bedspaceArchiveHistory.bedspaceId == bedspace.id }
          ?.actions ?: emptyList()
        cas3BedspaceTransformer.transformJpaToApi(bedspace, archiveHistory)
      },
      totalOnlineBedspaces = bedspaces.count { it.isCas3BedspaceOnline() },
      totalUpcomingBedspaces = bedspaces.count { it.isCas3BedspaceUpcoming() },
      totalArchivedBedspaces = bedspaces.count { it.isCas3BedspaceArchived() },
    )

    return ResponseEntity.ok(result)
  }

  @Transactional
  @PostMapping("/premises/{premisesId}/archive")
  fun archivePremises(
    @PathVariable premisesId: UUID,
    @RequestBody body: Cas3ArchivePremises,
  ): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val archivedPremises = extractEntityFromCasResult(
      cas3PremisesService.archivePremises(premises, body.endDate),
    )

    return ResponseEntity.ok(cas3PremisesTransformer.transformDomainToApi(archivedPremises))
  }

  @Transactional
  @PostMapping("/premises/{premisesId}/unarchive")
  fun unarchivePremises(
    @PathVariable premisesId: UUID,
    @RequestBody body: Cas3UnarchivePremises,
  ): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val unarchivedPremises = extractEntityFromCasResult(
      cas3PremisesService.unarchivePremises(premises, body.restartDate),
    )

    return ResponseEntity.ok(cas3PremisesTransformer.transformDomainToApi(unarchivedPremises))
  }

  @Suppress("ThrowsCount")
  @GetMapping("/premises/{premisesId}/bedspaces/{bedspaceId}")
  fun getPremisesBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val bedspace = extractEntityFromCasResult(cas3PremisesService.getBedspace(premises.id, bedspaceId))
    val archiveHistory = extractEntityFromCasResult(cas3PremisesService.getBedspaceArchiveHistory(bedspaceId))

    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(bedspace, archiveHistory))
  }

  @PostMapping("/premises/{premisesId}/bedspaces")
  fun createBedspace(
    @PathVariable premisesId: UUID,
    @RequestBody newBedspace: Cas3NewBedspace,
  ): ResponseEntity<Cas3Bedspace> {
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
  @PutMapping("/premises/{premisesId}/bedspaces/{bedspaceId}")
  fun updateBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @RequestBody updateBedspace: Cas3UpdateBedspace,
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

  @Transactional
  @PostMapping("/premises/{premisesId}/bedspaces/{bedspaceId}/archive")
  fun archiveBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @RequestBody body: Cas3ArchiveBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val archivedBedspace = extractEntityFromCasResult(
      cas3PremisesService.archiveBedspace(bedspaceId, premises, body.endDate),
    )

    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(archivedBedspace))
  }

  @Transactional
  @PostMapping("/premises/{premisesId}/bedspaces/{bedspaceId}/unarchive")
  fun unarchiveBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @RequestBody body: Cas3UnarchiveBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val unarchivedBedspace = extractEntityFromCasResult(
      cas3PremisesService.unarchiveBedspace(premises, bedspaceId, body.restartDate),
    )

    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(unarchivedBedspace))
  }

  @GetMapping("/premises/summary")
  fun getPremisesSummary(
    @RequestParam postcodeOrAddress: String?,
    @RequestParam sortBy: Cas3PremisesSortBy?,
  ): ResponseEntity<List<Cas3PremisesSummary>> {
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

  @GetMapping("/premises/search")
  fun searchPremises(
    @RequestParam postcodeOrAddress: String?,
    @RequestParam premisesStatus: Cas3PremisesStatus?,
  ): ResponseEntity<Cas3PremisesSearchResults> {
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

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/departures")
  fun postPremisesBookingDeparture(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody body: Cas3NewDeparture,
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

  @GetMapping("/premises/{premisesId}/future-bookings")
  fun getPremisesFutureBookings(
    @PathVariable premisesId: UUID,
    @RequestParam statuses: List<BookingStatus>,
  ): ResponseEntity<List<FutureBooking>> {
    val user = userService.getUserForRequest()

    val result = cas3BookingService.findFutureBookingsForPremises(premisesId, statuses, user)

    val futureBookings = extractEntityFromCasResult(result)

    return ResponseEntity.ok(
      futureBookings.map { fb -> cas3FutureBookingTransformer.transformJpaToApi(fb.booking, fb.personInfo) },
    )
  }

  @PutMapping("/premises/{premisesId}/bedspaces/{bedspaceId}/cancel-unarchive")
  fun cancelUnarchiveBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val result = extractEntityFromCasResult(
      cas3PremisesService.cancelUnarchiveBedspace(bedspaceId),
    )

    return ResponseEntity.ok(
      cas3BedspaceTransformer.transformJpaToApi(result),
    )
  }

  @PutMapping("/premises/{premisesId}/bedspaces/{bedspaceId}/cancel-archive")
  fun cancelArchiveBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val result = extractEntityFromCasResult(
      cas3PremisesService.cancelArchiveBedspace(premises, bedspaceId),
    )

    return ResponseEntity.ok(
      cas3BedspaceTransformer.transformJpaToApi(result),
    )
  }

  @PutMapping("/premises/{premisesId}/cancel-archive")
  fun cancelScheduledArchivePremises(
    @PathVariable premisesId: UUID,
  ): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val updatedPremises = extractEntityFromCasResult(
      cas3PremisesService.cancelScheduledArchivePremises(premisesId),
    )

    return ResponseEntity.ok(
      cas3PremisesTransformer.transformDomainToApi(updatedPremises),
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
