package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PersonService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import java.util.UUID

@Service
class PremisesController(
  private val premisesService: PremisesService,
  private val personService: PersonService,
  private val premisesTransformer: PremisesTransformer,
  private val bookingTransformer: BookingTransformer
) : PremisesApiDelegate {
  override fun premisesGet(): ResponseEntity<List<Premises>> {
    return ResponseEntity.ok(
      premisesService.getAllPremises().map(premisesTransformer::transformJpaToApi)
    )
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises))
  }

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Booking>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(
      premises.bookings.map {
        val person = personService.getPerson(it.crn)
          ?: throw InternalServerErrorProblem("Unable to get Person via crn: ${it.crn}")
        bookingTransformer.transformJpaToApi(it, person)
      }
    )
  }
}
