package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementRequestsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPersonDetailsForCrn
import java.util.UUID

@Service
class PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: PlacementRequestService,
  private val placementRequestTransformer: PlacementRequestTransformer,
  private val offenderService: OffenderService
) : PlacementRequestsApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun placementRequestsGet(): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    val requests = placementRequestService.getVisiblePlacementRequestsForUser(user)

    return ResponseEntity.ok(
      requests.mapNotNull {
        val personDetail = getPersonDetailsForCrn(log, it.application.crn, user.deliusUsername, offenderService)

        if (personDetail === null) {
          return@mapNotNull null
        }

        placementRequestTransformer.transformJpaToApi(it, personDetail.first, personDetail.second)
      }
    )
  }

  override fun placementRequestsIdGet(id: UUID): ResponseEntity<PlacementRequest> {
    val user = userService.getUserForRequest()

    val authorisationResult = placementRequestService.getPlacementRequestForUser(user, id)

    val placementRequest = when (authorisationResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "PlacementRequest")
      is AuthorisableActionResult.Success -> authorisationResult.entity
    }

    val personDetail = getPersonDetailsForCrn(log, placementRequest.application.crn, user.deliusUsername, offenderService)
      ?: throw NotFoundProblem(placementRequest.application.crn, "Offender")

    return ResponseEntity.ok(
      placementRequestTransformer.transformJpaToApi(placementRequest, personDetail.first, personDetail.second)
    )
  }
}
