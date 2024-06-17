package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas3

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.toJson
import java.time.LocalDate
import java.util.UUID

fun createTemporaryAccommodationApplication(
  crn: (Session) -> String = { _ -> CRN },
  saveApplicationIdAs: String? = null,
) = CoreDsl.exec(
  HttpDsl.http("Create Application")
    .post("/applications")
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .body(
      toJson { session ->
        NewApplication(
          crn = crn(session),
          convictionId = 0L,
          deliusEventNumber = "",
          offenceId = "",
        )
      },
    )
    .check(HttpDsl.status().`is`(201))
    .let {
      when (saveApplicationIdAs) {
        null -> it
        else -> it.check(CoreDsl.jsonPath("$.id").saveAs(saveApplicationIdAs))
      }
    },
)

fun updateTemporaryAccommodationApplication(
  applicationId: (Session) -> UUID,
) = CoreDsl.exec(
  HttpDsl.http("Update Application")
    .put { session -> "/applications/${applicationId(session)}" }
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .body(
      toJson(
        UpdateTemporaryAccommodationApplication(
          type = UpdateApplicationType.CAS3,
          data = mapOf(),
        ),
      ),
    ),
)

fun submitTemporaryAccommodationApplication(
  applicationId: (Session) -> UUID,
  arrivalDate: (Session) -> LocalDate = { _ -> LocalDate.now() },
) = CoreDsl.exec(
  HttpDsl.http("Submit Application")
    .post { session -> "/applications/${applicationId(session)}/submission" }
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .body(
      toJson { session ->
        SubmitTemporaryAccommodationApplication(
          arrivalDate = arrivalDate(session),
          type = "CAS3",
          translatedDocument = "{}",
          summaryData = "{}",
        )
      },
    ),
)
