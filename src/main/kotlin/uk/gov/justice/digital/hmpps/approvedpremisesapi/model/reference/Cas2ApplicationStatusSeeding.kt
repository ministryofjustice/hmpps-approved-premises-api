package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

object Cas2ApplicationStatusSeeding {

  fun statusList(): List<Cas2PersistedApplicationStatus> {
    return listOf(
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
        name = "moreInfoRequested",
        label = "More information requested",
        description = "The prison offender manager (POM) must provide information requested for the application to progress.",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1"),
        name = "awaitingDecision",
        label = "Awaiting decision",
        description = "The CAS-2 team has the information they need and will make a decision.",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"),
        name = "placeOffered",
        label = "Place offered",
        description = "The applicant has been offered a place for Short-Term Accommodation (CAS-2).",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("a919097d-b324-471c-9834-756f255e87ea"),
        name = "onWaitingList",
        label = "On waiting list",
        description = "The applicant has been added to the waiting list for Short-Term Accommodation (CAS-2).",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("758eee61-2a6d-46b9-8bdd-869536d77f1b"),
        name = "noPlaceOffered",
        label = "Could not be placed",
        description = "The applicant could not be placed in Short-Term Accommodation (CAS-2).",
        isActive = false,
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("4ad9bbfa-e5b0-456f-b746-146f7fd511dd"),
        name = "incomplete",
        label = "Incomplete",
        description = "The application could not progress because the prison offender manager (POM) did not provide the requested information.",
        isActive = false,
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"),
        name = "withdrawn",
        label = "Referral withdrawn",
        description = "The prison offender manager (POM) withdrew the application.",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019"),
        name = "offerAccepted",
        label = "Offer accepted",
        description = "The accommodation offered has been accepted.",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"),
        name = "offerDeclined",
        label = "Offer declined or withdrawn",
        description = "The accommodation offered has been declined or withdrawn.",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
        name = "cancelled",
        label = "Referral cancelled",
        description = "The application has been cancelled.",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565"),
        name = "awaitingArrival",
        label = "Awaiting arrival",
        description = "The accommodation is arranged for the agreed dates.",
      ),
    )
  }
}
