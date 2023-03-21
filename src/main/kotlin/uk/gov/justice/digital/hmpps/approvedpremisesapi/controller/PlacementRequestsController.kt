package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementRequestsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPersonDetailsForCrn

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
}
