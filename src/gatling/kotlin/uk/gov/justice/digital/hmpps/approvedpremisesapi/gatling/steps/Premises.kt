package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.toJson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

fun createTemporaryAccommodationPremises(
  probationRegionId: (Session) -> UUID,
  probationDeliveryUnitId: (Session) -> UUID,
  savePremisesIdAs: String? = null,
) = CoreDsl.exec(
  HttpDsl.http("Create Premises")
    .post("/premises")
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .body(
      toJson { session ->
        NewPremises(
          name = randomStringMultiCaseWithNumbers(16),
          addressLine1 = randomStringMultiCaseWithNumbers(16),
          postcode = randomPostCode(),
          probationRegionId = probationRegionId(session),
          characteristicIds = listOf(),
          status = PropertyStatus.ACTIVE,
          probationDeliveryUnitId = probationDeliveryUnitId(session),
        )
      },
    )
    .check(HttpDsl.status().`is`(201))
    .let {
      when (savePremisesIdAs) {
        null -> it
        else -> it.check(CoreDsl.jsonPath("$.id").saveAs(savePremisesIdAs))
      }
    },
)

fun createTemporaryAccommodationRoom(
  premisesId: (Session) -> UUID,
  saveRoomIdAs: String? = null,
  saveBedIdAs: String? = null,
) = CoreDsl.exec(
  HttpDsl.http("Create Room")
    .post { session -> "/premises/${premisesId(session)}/rooms" }
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .body(
      toJson(
        NewRoom(
          name = randomStringUpperCase(8),
          characteristicIds = listOf(),
          notes = randomStringMultiCaseWithNumbers(20),
        ),
      ),
    )
    .check(HttpDsl.status().`is`(201))
    .let {
      when (saveRoomIdAs) {
        null -> it
        else -> it.check(CoreDsl.jsonPath("$.id").saveAs(saveRoomIdAs))
      }
    }
    .let {
      when (saveBedIdAs) {
        null -> it
        else -> it.check(CoreDsl.jsonPath("$.beds[0].id").saveAs(saveBedIdAs))
      }
    },
)
