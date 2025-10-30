package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import arrow.core.Ior
import jakarta.transaction.Transactional
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer

@Service
class PremisesController(
  private val userAccessService: UserAccessService,
  private val premisesService: PremisesService,
  private val bookingService: BookingService,
  private val premisesTransformer: PremisesTransformer,
  private val bookingTransformer: BookingTransformer,

) : PremisesApiDelegate {
  @Transactional
  override fun premisesPremisesIdPut(premisesId: UUID, body: UpdatePremises): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val serviceName = when (premises) {
      is TemporaryAccommodationPremisesEntity -> throw BadRequestProblem(errorDetail = "This Api endpoint does not support updating premises of type Temporary Accommodation use /cas3/premises/{premisesId} Api endpoint.")
      else -> ServiceName.approvedPremises
    }

    if (!userAccessService.currentUserCanManagePremises(premises) || !userAccessService.currentUserCanAccessRegion(serviceName, body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val updatePremisesResult =
      premisesService
        .updatePremises(
          premisesId,
          body.addressLine1,
          body.addressLine2,
          body.town,
          body.postcode,
          body.localAuthorityAreaId,
          body.probationRegionId,
          body.characteristicIds,
          body.notes,
          body.status,
        )

    var validationResult = when (updatePremisesResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(premisesId, "Premises")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updatePremisesResult.entity
    }

    val updatedPremises = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    val totalBeds = premisesService.getBedCount(updatedPremises)

    return ResponseEntity.ok(
      premisesTransformer.transformJpaToApi(
        updatedPremises,
        totalBeds = totalBeds,
        availableBedsForToday = totalBeds,
      ),
    )
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val totalBeds: Int
    val availableBedsForToday: Int

    when (premises) {
      is TemporaryAccommodationPremisesEntity -> {
        throw BadRequestProblem(errorDetail = "This Api endpoint does not support Temporary Accommodation use /cas3/premises/{premisesId} Api endpoint.")
      }
      else -> {
        totalBeds = premisesService.getBedCount(premises)
        availableBedsForToday =
          premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
            .values.first().getFreeCapacity(totalBeds)
      }
    }

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises, totalBeds, availableBedsForToday))
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val bookingResult = bookingService.getBooking(bookingId)

    val bookingAndPersons = when (bookingResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bookingResult.id!!, bookingResult.entityType!!)
      is AuthorisableActionResult.Success -> bookingResult.entity
    }

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
    )

    if (apiBooking.premises.id != premisesId) {
      throw NotFoundProblem(premisesId, "Premises")
    }

    return ResponseEntity.ok(apiBooking)
  }

  private fun <EntityType> extractResultEntityOrThrow(result: ValidatableActionResult<EntityType>) = when (result) {
    is ValidatableActionResult.Success -> result.entity
    is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
    is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages.mapValues { ParamDetails(it.value) })
    is ValidatableActionResult.ConflictError -> throw ConflictProblem(
      id = result.conflictingEntityId,
      conflictReason = result.message,
    )
  }

}
