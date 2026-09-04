package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1AuthorisedPlacementPeriod

data class Cas1PlacementApplicationDecisionAcceptanceDto(
  val authorisedPlacementPeriod: Cas1AuthorisedPlacementPeriod
)