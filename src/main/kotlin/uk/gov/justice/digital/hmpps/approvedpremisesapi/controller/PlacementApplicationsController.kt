package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PlacementApplicationsController
import java.util.UUID

@Service
class PlacementApplicationsController(
  private val cas1PlacementApplicationsController: Cas1PlacementApplicationsController,
) : PlacementApplicationsApiDelegate {
  override fun placementApplicationsPost(newPlacementApplication: NewPlacementApplication): ResponseEntity<PlacementApplication> = cas1PlacementApplicationsController.placementApplicationsPost(newPlacementApplication)

  override fun placementApplicationsIdGet(id: UUID): ResponseEntity<PlacementApplication> = cas1PlacementApplicationsController.placementApplicationsIdGet(id)

  override fun placementApplicationsIdPut(
    id: UUID,
    updatePlacementApplication: UpdatePlacementApplication,
  ): ResponseEntity<PlacementApplication> = cas1PlacementApplicationsController.placementApplicationsIdPut(id, updatePlacementApplication)

  override fun placementApplicationsIdSubmissionPost(
    id: UUID,
    submitPlacementApplication: SubmitPlacementApplication,
  ): ResponseEntity<List<PlacementApplication>> = cas1PlacementApplicationsController.placementApplicationsIdSubmissionPost(id, submitPlacementApplication)

  override fun placementApplicationsIdDecisionPost(
    id: UUID,
    placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ): ResponseEntity<PlacementApplication> = cas1PlacementApplicationsController.placementApplicationsIdDecisionPost(id, placementApplicationDecisionEnvelope)

  override fun placementApplicationsIdWithdrawPost(
    id: UUID,
    withdrawPlacementApplication: WithdrawPlacementApplication?,
  ): ResponseEntity<PlacementApplication> = cas1PlacementApplicationsController.placementApplicationsIdWithdrawPost(id, withdrawPlacementApplication)
}
