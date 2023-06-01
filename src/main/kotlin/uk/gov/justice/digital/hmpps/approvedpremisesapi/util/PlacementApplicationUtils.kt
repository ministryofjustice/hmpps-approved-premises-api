package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun <T> mapAndTransformPlacementApplications(
  log: Logger,
  placementApplications: List<PlacementApplicationEntity>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (PlacementApplicationEntity, OffenderDetailSummary, InmateDetail) -> T,
): List<T> {
  return placementApplications.mapNotNull {
    val placementApplication = transformPlacementApplication<T>(log, it, deliusUsername, offenderService, transformer)
      ?: return@mapNotNull null

    placementApplication
  }
}

fun <T> transformPlacementApplication(
  log: Logger,
  placementApplication: PlacementApplicationEntity,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (PlacementApplicationEntity, OffenderDetailSummary, InmateDetail) -> T,
): T {
  val (offenderDetailSummary, inmateDetail) = getPersonDetailsForCrn(log, placementApplication.application.crn, deliusUsername, offenderService)
    ?: throw NotFoundProblem(placementApplication.application.crn, "Offender")

  return transformer(placementApplication, offenderDetailSummary, inmateDetail)
}
