package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas3

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

fun getProbationDeliveryUnit(
  probationRegionId: (Session) -> UUID,
  saveProbationDeliveryUnitIdAs: String,
) = CoreDsl.exec(

  HttpDsl.http("Get Probation Delivery Unit")
    .get("/reference-data/probation-delivery-units")
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .queryParam("probationRegionId") { session -> probationRegionId(session) }
    .check(HttpDsl.status().`is`(200))
    .check(CoreDsl.jsonPath("$[0].id").saveAs(saveProbationDeliveryUnitIdAs)),
)
