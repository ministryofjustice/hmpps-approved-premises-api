package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

object Cas2ApplicationStatusSeeding {

  @SuppressWarnings("LongMethod")
  fun statusList(): List<Cas2PersistedApplicationStatus> {
    return listOf(
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
        name = "moreInfoRequested",
        label = "More information requested",
        description = "The prison offender manager (POM) must provide information requested for the application to progress.",
        statusDetails = listOf(
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"),
            name = "personalInformation",
            label = "Personal information",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("05669c8a-d65c-48d2-a5e4-0c3f6fc8977b"),
            name = "exclusionZonesAndAreas",
            label = "Exclusion zones and preferred areas",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("831c241d-63f3-4d17-b969-b8154d7e4902"),
            name = "healthNeeds",
            label = "Health needs",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("7ba5cd7d-8ae3-4fe5-bb27-9367197ea160"),
            name = "riskToSelf",
            label = "Risk to self",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("8641d719-7356-42d3-8363-e323bf76caec"),
            name = "riskOfSeriousHarm",
            label = "Risk of serious harm",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("90f075ae-0b9f-445b-a9b5-1095abca87dc"),
            name = "currentOffences",
            label = "Current offences",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("8ce77ea1-324e-4ac8-be8c-33d6d4d927f8"),
            name = "offendingHistory",
            label = "Offending history",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("d2c44705-2795-4610-9879-dcf26940e121"),
            name = "hdcAndCpp",
            label = "HDC licence and CPP details",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("94631a70-6c51-43d6-9112-2b6d042b5aa0"),
            name = "other",
            label = "Other",
          ),
        ),
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
            label = "HDC - no longer eligible",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("c5dce0d2-fc05-4a07-8157-25b8821cdb06"),
            name = "governorDecidedUnsuitable",
            label = "Governor - decided unsuitable",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("3fb37f85-be88-4eee-812d-af122e268eef"),
            name = "governorChosenAlternative",
            label = "Governor - chosen alternative accommodation",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("8a857a7d-94f9-43ec-963c-2a2528e88a6e"),
            name = "governorOther",
            label = "Governor - other",
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
            id = UUID.fromString("ba46bbe0-8fb6-4539-895d-5586e6bfe8b6"),
            name = "assessedAsHighRisk",
            label = "Assessed as high risk",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("522bb736-aeb6-480f-a51a-2bf3dcfcd482"),
            name = "notEligible",
            label = "Not eligible",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("ccf43af1-359b-4a14-8941-85eefa88f016"),
            name = "noRecourseToPublicFunds",
            label = "No recourse to public funds",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("c149a14d-ba06-420a-b844-5edfc02da6b1"),
            name = "noPropertyAvailable",
            label = "No property available",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("3fbdccc9-4858-4ae4-abb5-bd2b90d96d96"),
            name = "noFemalePropertyAvailable",
            label = "No female property available",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("bc539d6d-c353-49fa-847f-6967a148c527"),
            name = "noAdaptedPropertyAvailable",
            label = "No adapted property available",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("78636840-0155-45d4-971e-fe8d2d6c660c"),
            name = "noSuitablePropertyAvailable",
            label = "No suitable property available",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("7e8749c9-5254-4dae-90ed-590cf9f59847"),
            name = "incompleteReferral",
            label = "Incomplete referral",
          ),
          Cas2PersistedApplicationStatusDetail(
            id = UUID.fromString("d1d96185-d92a-450b-b47f-bcce50356eed"),
            name = "createdInError",
            label = "Created in error",
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
