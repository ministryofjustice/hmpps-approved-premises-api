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
        id = UUID.fromString("a919097d-b324-471c-9834-756f255e87ea"),
        name = "onWaitingList",
        label = "On waiting list",
        description = "The applicant has been added to the waiting list for Short-Term Accommodation (CAS-2).",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"),
        name = "placeOffered",
        label = "Place offered",
        description = "The applicant has been offered a place for Short-Term Accommodation (CAS-2).",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019"),
        name = "offerAccepted",
        label = "Offer accepted",
        description = "The accommodation offered has been accepted.",
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
        id = UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"),
        name = "offerDeclined",
        label = "Offer declined or withdrawn",
        description = "The accommodation offered has been declined or withdrawn.",
        statusDetails = listOf(
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("62645779-242d-4601-a8f8-d2cbf1d41dfa"),
            name = "areaUnsuitable",
            label = "Area unsuitable",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("fc38f750-e9d2-4270-b542-d38286b9855c"),
            name = "changeOfCircumstances",
            label = "Change of circumstances",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("31122b89-e087-4b5f-b59a-f7ffa0dd3e0c"),
            name = "noResponse",
            label = "No response",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("32e62af3-6ea5-4496-a82c-b7bad67080a5"),
            name = "offerWithdrawnByNacro",
            label = "Offer withdrawn by Nacro",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("b5bfbff4-aaa6-4fb0-ba36-5bca58927dc5"),
            name = "propertyUnsuitable",
            label = "Property unsuitable for needs",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("f58267b8-f91b-4a4f-9aa2-80089ba111e4"),
            name = "withdrawnByReferrer",
            label = "Withdrawn by referrer",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("ed5d529c-d7d1-4f29-a0c0-89fd104cc320"),
            name = "rehousedByAnotherLandlord",
            label = "Rehoused by another landlord",
          ),
        ),
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"),
        name = "withdrawn",
        label = "Referral withdrawn",
        description = "The prison offender manager (POM) withdrew the application.",
        statusDetails = listOf(
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("e4a2391e-e847-427a-a913-51e0b0ad9f52"),
            name = "hdcNoLongerEligible",
            label = "HDC - No longer eligible",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("c5dce0d2-fc05-4a07-8157-25b8821cdb06"),
            name = "governorDecidedUnsuitable",
            label = "Governor - Decided unsuitable",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("3fb37f85-be88-4eee-812d-af122e268eef"),
            name = "governorChosenAlternative",
            label = "Governor - Chosen alternative accommodation",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("8a857a7d-94f9-43ec-963c-2a2528e88a6e"),
            name = "governorOther",
            label = "Governor - Other",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("d3c789b8-947d-4e24-9cef-335545d85abe"),
            name = "withdrewOrDeclinedOffer",
            label = "Withdrew or declined offer",
          ),
        ),
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
        name = "cancelled",
        label = "Referral cancelled",
        description = "The application has been cancelled.",
        statusDetails = listOf(
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "assessedAsHighRisk",
            label = "Assessed as High Risk",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "notEligible",
            label = "Not Eligible",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
            name = "noRecourseToPublicFunds",
            label = "No Recourse to Public Funds",
          ),
        ),
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
