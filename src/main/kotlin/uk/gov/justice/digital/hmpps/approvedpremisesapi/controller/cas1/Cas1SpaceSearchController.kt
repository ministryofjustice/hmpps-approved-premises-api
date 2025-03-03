package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceSearchesCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResults

@Service
class Cas1SpaceSearchController(
  private val cas1PremisesController: Cas1PremisesController,
) : SpaceSearchesCas1Delegate {
  override fun spaceSearch(cas1SpaceSearchParameters: Cas1PremisesSearchParameters): ResponseEntity<Cas1PremisesSearchResults> = cas1PremisesController.premisesSearch(cas1SpaceSearchParameters)
}
