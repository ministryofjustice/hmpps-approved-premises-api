package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun <T> mapAndTransformPlacementRequests(
  log: Logger,
  placementRequests: List<PlacementRequestEntity>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (PlacementRequestEntity, OffenderDetailSummary, InmateDetail) -> T,
): List<T> {
  return placementRequests.mapNotNull {
    val assessment = transformPlacementRequest<T>(log, it, deliusUsername, offenderService, transformer)
      ?: return@mapNotNull null

    assessment
  }
}

fun <T> transformPlacementRequest(
  log: Logger,
  placementRequest: PlacementRequestEntity,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (PlacementRequestEntity, OffenderDetailSummary, InmateDetail) -> T,
): T? {
  val personDetail = getPersonDetailsForCrn(log, placementRequest.application.crn, deliusUsername, offenderService)
    ?: return null

  return transformer(placementRequest, personDetail.first, personDetail.second)
}
