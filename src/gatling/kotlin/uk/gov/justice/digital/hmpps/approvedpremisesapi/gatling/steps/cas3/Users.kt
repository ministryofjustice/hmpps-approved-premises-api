package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas3

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

fun getUserProbationRegion(
  saveProbationRegionIdAs: String,
) = CoreDsl.exec(
  HttpDsl.http("Get User's Probation Region")
    .get("/profile")
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .check(HttpDsl.status().`is`(200))
    .check(CoreDsl.jsonPath("$.region.id").saveAs(saveProbationRegionIdAs)),
)
