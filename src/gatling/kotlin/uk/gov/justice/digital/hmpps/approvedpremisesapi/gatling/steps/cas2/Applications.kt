package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas2

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.toJson
import java.time.LocalDate
import java.util.UUID

fun createCas2Application(
  crn: (Session) -> String = { _ -> CRN },
  saveApplicationIdAs: String? = null,
) = CoreDsl.exec(
  HttpDsl.http("Create CAS2 Application")
    .post("/cas2/applications")
    .header("X-Service-Name", ServiceName.cas2.value)
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

fun updateCas2Application(
  applicationId: (Session) -> UUID,
) = CoreDsl.exec(
  HttpDsl.http("Update Application")
    .put { session -> "/cas2/applications/${applicationId(session)}" }
    .header("X-Service-Name", ServiceName.cas2.value)
    .body(
      toJson(
        UpdateCas2Application(
          type = UpdateApplicationType.CAS2,
          data = mapOf(),
        ),
      ),
    ),
)

fun submitCas2Application(
  applicationId: (Session) -> UUID,
) = CoreDsl.exec(
  HttpDsl.http("Submit Application")
    .post { session -> "/cas2/submissions" }
    .header("X-Service-Name", ServiceName.cas2.value)
    .body(
      toJson { session ->
        SubmitCas2Application(
          applicationId = applicationId(session),
          translatedDocument = {},
          telephoneNumber = "123 456 7891",
          preferredAreas = "Leeds | Bradford",
          hdcEligibilityDate = LocalDate.parse("2023-03-30"),
          conditionalReleaseDate = LocalDate.parse("2023-04-29"),
        )
      },
    ),
)
