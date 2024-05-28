package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.LostBedsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LostBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import java.time.LocalDate
import java.util.UUID

@Service
class LostBedsController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val premisesService: PremisesService,
  private val bookingService: BookingService,
  private val lostBedsService: LostBedService,
  private val lostBedsTransformer: LostBedsTransformer,
  private val lostBedCancellationTransformer: LostBedCancellationTransformer,
) : LostBedsCas1Delegate {

  override fun premisesPremisesIdLostBedsPost(premisesId: UUID, body: NewLostBed): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    throwIfLostBedDatesConflict(body.startDate, body.endDate, null, body.bedId)

    val result = premisesService.createLostBeds(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedId = body.bedId,
    )

    val lostBeds = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBeds))
  }

  override fun premisesPremisesIdLostBedsGet(premisesId: UUID): ResponseEntity<List<LostBed>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val lostBeds = lostBedsService.getActiveLostBedsForPremisesId(premisesId)

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(lostBeds.map(lostBedsTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdLostBedsLostBedIdGet(premisesId: UUID, lostBedId: UUID): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId }
      ?: throw NotFoundProblem(lostBedId, "LostBed")

    val user = usersService.getUserForRequest()

    if (!user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBed))
  }

  override fun premisesPremisesIdLostBedsLostBedIdPut(
    premisesId: UUID,
    lostBedId: UUID,
    body: UpdateLostBed,
  ): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "LostBed")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, lostBed.bed.id)
    throwIfLostBedDatesConflict(body.startDate, body.endDate, lostBedId, lostBed.bed.id)

    val updateLostBedResult = premisesService
      .updateLostBeds(
        lostBedId,
        body.startDate,
        body.endDate,
        body.reason,
        body.referenceNumber,
        body.notes,
      )

    val validationResult = when (updateLostBedResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(lostBedId, "LostBed")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updateLostBedResult.entity
    }

    val updatedLostBed = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(updatedLostBed))
  }

  override fun premisesPremisesIdLostBedsLostBedIdCancellationsPost(
    premisesId: UUID,
    lostBedId: UUID,
    body: NewLostBedCancellation,
  ): ResponseEntity<LostBedCancellation> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "LostBed")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    val cancelLostBedResult = premisesService.cancelLostBed(
      lostBed = lostBed,
      notes = body.notes,
    )

    val cancellation = when (cancelLostBedResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = cancelLostBedResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = cancelLostBedResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = cancelLostBedResult.conflictingEntityId, conflictReason = cancelLostBedResult.message)
      is ValidatableActionResult.Success -> cancelLostBedResult.entity
    }

    return ResponseEntity.ok(lostBedCancellationTransformer.transformJpaToApi(cancellation))
  }

  private fun <EntityType> extractResultEntityOrThrow(result: ValidatableActionResult<EntityType>) = when (result) {
    is ValidatableActionResult.Success -> result.entity
    is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
    is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages)
    is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = result.conflictingEntityId, conflictReason = result.message)
  }

  private fun throwIfBookingDatesConflict(
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedId: UUID,
  ) {
    bookingService.getBookingWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
      throw ConflictProblem(it.id, "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates")
    }
  }

  private fun throwIfLostBedDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getLostBedWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates")
    }
  }
}
