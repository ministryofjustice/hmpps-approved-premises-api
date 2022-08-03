package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ProbationRegion
import java.util.UUID

@Service
class PremisesController : PremisesApiDelegate {
  override fun premisesGet(): ResponseEntity<List<Premises>> {
    return ResponseEntity.ok(
      listOf(
        Premises(
          id = UUID.fromString("efd52239-f5cb-4b00-ab1a-9a4e3402fc58"),
          name = "Beckenham Road",
          apCode = "BELON",
          postcode = "BR3 4LR",
          bedCount = 20,
          probationRegion = ProbationRegion(
            id = "LON",
            name = "London"
          ),
          apArea = ApArea(
            id = "LON",
            name = "London"
          ),
          localAuthorityArea = LocalAuthorityArea(
            id = "CAM",
            name = "Camden"
          )
        )
      )
    )
  }
}
