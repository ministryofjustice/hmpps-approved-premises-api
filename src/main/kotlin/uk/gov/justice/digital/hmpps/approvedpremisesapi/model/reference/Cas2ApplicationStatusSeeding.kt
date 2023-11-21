package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import java.util.UUID

object Cas2ApplicationStatusSeeding {

  fun getById(id: UUID): Cas2ApplicationStatus {
    return statusList().find { status -> status.id == id }
      ?: error("Status with id $id not found")
  }

  fun statusList(): List<Cas2ApplicationStatus> {
    return listOf(
      Cas2ApplicationStatus(
        id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
        name = "moreInfoRequested",
        label = "More information requested",
        description = "The prison offender manager (POM) must provide information requested for the application to progress.",
      ),
      Cas2ApplicationStatus(
        id = UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1"),
        name = "awaitingDecision",
        label = "Awaiting decision",
        description = "The CAS-2 team has the information they need and will make a decision.",
      ),
      Cas2ApplicationStatus(
        id = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"),
        name = "placeOffered",
        label = "Place offered",
        description = "The applicant has been offered a place for Short-Term Accommodation (CAS-2).",
      ),
      Cas2ApplicationStatus(
        id = UUID.fromString("a919097d-b324-471c-9834-756f255e87ea"),
        name = "onWaitingList",
        label = "On waiting list",
        description = "The applicant has been added to the waiting list for Short-Term Accommodation (CAS-2).",
      ),
      Cas2ApplicationStatus(
        id = UUID.fromString("758eee61-2a6d-46b9-8bdd-869536d77f1b"),
        name = "noPlaceOffered",
        label = "Could not be placed",
        description = "The applicant could not be placed in Short-Term Accommodation (CAS-2).",
      ),
      Cas2ApplicationStatus(
        id = UUID.fromString("4ad9bbfa-e5b0-456f-b746-146f7fd511dd"),
        name = "incomplete",
        label = "Incomplete",
        description = "The application could not progress because the prison offender manager (POM) did not provide the requested information.",
      ),
      Cas2ApplicationStatus(
        id = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"),
        name = "withdrawn",
        label = "Withdrawn",
        description = "The prison offender manager (POM) withdrew the application.",
      ),
    )
  }
}
