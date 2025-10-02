package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

object Cas2ApplicationStatusSeeding {

  val statuses = listOf(
    Cas2PersistedApplicationStatus(
      id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
      name = "moreInfoRequested",
      label = "More information requested",
      description = "The referrer must provide information requested for the application to progress.",
      statusDetails = listOf(
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"),
          name = "personalInformation",
          label = "Personal information",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"),
          name = "applicantDetails",
          label = "Applicant details",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("28926b7b-2056-478c-95fe-a611967b8fab"),
          name = "concernsToOthers",
          label = "Concerns to others",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("76be117e-c496-489c-a69d-ef26a05a6f64"),
          name = "concernsToTheApplicant",
          label = "Concerns to the applicant",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("03718bee-a0f1-4a5d-b095-e23c2b1211ab"),
          name = "currentAllegedOffences",
          label = "Current alleged offences",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("53f17f5b-c3bb-4bd2-9ccb-b5f0664aa104"),
          name = "previousUnspentConvictions",
          label = "Previous unspent convictions",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("4c8e39ea-0f8b-4131-a781-cac706f38914"),
          name = "probationAndOASys",
          label = "Probation & OASys",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("0e78cbc4-4139-4402-8a8a-763d9ca5d93b"),
          name = "fundingAndID",
          label = "Funding & ID",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("05669c8a-d65c-48d2-a5e4-0c3f6fc8977b"),
          name = "exclusionZonesAndAreas",
          label = "Exclusion zones and preferred areas",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("831c241d-63f3-4d17-b969-b8154d7e4902"),
          name = "healthNeeds",
          label = "Health needs",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("7ba5cd7d-8ae3-4fe5-bb27-9367197ea160"),
          name = "riskToSelf",
          label = "Risk to self",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("8641d719-7356-42d3-8363-e323bf76caec"),
          name = "riskOfSeriousHarm",
          label = "Risk of serious harm",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("90f075ae-0b9f-445b-a9b5-1095abca87dc"),
          name = "currentOffences",
          label = "Current offences",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("8ce77ea1-324e-4ac8-be8c-33d6d4d927f8"),
          name = "offendingHistory",
          label = "Offending history",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("d2c44705-2795-4610-9879-dcf26940e121"),
          name = "hdcAndCpp",
          label = "HDC licence and CPP details",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("94631a70-6c51-43d6-9112-2b6d042b5aa0"),
          name = "other",
          label = "Other",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
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
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("fc38f750-e9d2-4270-b542-d38286b9855c"),
          name = "changeOfCircumstances",
          label = "Change of circumstances",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("31122b89-e087-4b5f-b59a-f7ffa0dd3e0c"),
          name = "noResponse",
          label = "No response",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("32e62af3-6ea5-4496-a82c-b7bad67080a5"),
          name = "offerWithdrawnByNacro",
          label = "Offer withdrawn by Nacro",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("b5bfbff4-aaa6-4fb0-ba36-5bca58927dc5"),
          name = "propertyUnsuitable",
          label = "Property unsuitable for needs",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("f58267b8-f91b-4a4f-9aa2-80089ba111e4"),
          name = "withdrawnByReferrer",
          label = "Withdrawn by referrer",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("ed5d529c-d7d1-4f29-a0c0-89fd104cc320"),
          name = "rehousedByAnotherLandlord",
          label = "Rehoused by another landlord",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("37aa6cab-1ef7-497e-8a8c-624c9ab69f5a"),
          name = "applicantBailedToNonCAS2Accommodation",
          label = "Applicant bailed to non CAS2 accommodation",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("566f7a9e-00c4-431d-9b3c-af87ff78379d"),
          name = "applicantRemandedInCustody",
          label = "Applicant remanded in custody",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("74397513-72f8-4efc-9ecf-32adf8c1db8a"),
          name = "applicantUnableToAffordRent",
          label = "Applicant unable to afford rent",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
      ),
    ),
    Cas2PersistedApplicationStatus(
      id = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"),
      name = "withdrawn",
      label = "Referral withdrawn",
      description = "The referrer withdrew the application.",
      statusDetails = listOf(
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("e4a2391e-e847-427a-a913-51e0b0ad9f52"),
          name = "hdcNoLongerEligible",
          label = "HDC - no longer eligible",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("c5dce0d2-fc05-4a07-8157-25b8821cdb06"),
          name = "governorDecidedUnsuitable",
          label = "Governor - decided unsuitable",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("3fb37f85-be88-4eee-812d-af122e268eef"),
          name = "governorChosenAlternative",
          label = "Governor - chosen alternative accommodation",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("8a857a7d-94f9-43ec-963c-2a2528e88a6e"),
          name = "governorOther",
          label = "Governor - other",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("d3c789b8-947d-4e24-9cef-335545d85abe"),
          name = "withdrewOrDeclinedOffer",
          label = "Withdrew or declined offer",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("6fc8d3b7-eb53-479d-8903-3880a9ed563f"),
          name = "personTransferredToAnotherPrisonWithdrawal",
          label = "Person transferred to another prison",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("3acf6010-820a-458d-ae8d-1d8a937af890"),
          name = "changeOfCircumstances",
          label = "Change of circumstances",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("cc5e63f7-c37a-46f5-a83a-5953e40881c6"),
          name = "sentencingHearingBooked",
          label = "Sentencing hearing booked",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("766a1675-b917-4532-8485-6ec0380f4f4c"),
          name = "applicantTransferredToAnotherPrisonWithdrawal",
          label = "Applicant transferred to another prison",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("c985337e-b9a7-4609-ac9b-fb8b5ec4a48a"),
          name = "applicantBailedToNonCAS2Accommodation",
          label = "Applicant bailed to non CAS2 accommodation",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("170787fa-8474-40e7-bdf7-c9f072ce06ef"),
          name = "applicantGivenCommunitySentence",
          label = "Applicant given community sentence",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("fec969b5-6cab-459e-a595-8337e43b34a8"),
          name = "applicantGivenCustodialSentence",
          label = "Applicant given custodial sentence",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("3440c6c1-b5ff-4b76-ae48-ecf80a3cec49"),
          name = "otherSentence",
          label = "Other sentence",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("cf339ef6-bcd7-47ae-8f54-b0011a46a3c8"),
          name = "applicantRemandedInCustody",
          label = "Applicant remanded in custody",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
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
          id = UUID.fromString("ba46bbe0-8fb6-4539-cccc-5586e6bfe8b6"),
          name = "nacroAssessedAsHighRisk",
          label = "NACRO assessed as high risk",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("ba46bbe0-8fb6-4539-895d-5586e6bfe8b6"),
          name = "assessedAsHighRisk",
          label = "Assessed as high risk",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("522bb736-aeb6-480f-a51a-2bf3dcfcd482"),
          name = "notEligible",
          label = "Not eligible",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("ccf43af1-359b-4a14-8941-85eefa88f016"),
          name = "noRecourseToPublicFunds",
          label = "No recourse to public funds",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("c149a14d-ba06-420a-b844-5edfc02da6b1"),
          name = "noPropertyAvailable",
          label = "No property available",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("3fbdccc9-4858-4ae4-abb5-bd2b90d96d96"),
          name = "noFemalePropertyAvailable",
          label = "No female property available",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("bc539d6d-c353-49fa-847f-6967a148c527"),
          name = "noAdaptedPropertyAvailable",
          label = "No adapted property available",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("78636840-0155-45d4-971e-fe8d2d6c660c"),
          name = "noSuitablePropertyAvailable",
          label = "No suitable property available",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("7e8749c9-5254-4dae-90ed-590cf9f59847"),
          name = "incompleteReferral",
          label = "Incomplete referral",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("d1d96185-d92a-450b-b47f-bcce50356eed"),
          name = "createdInError",
          label = "Created in error",
          applicableToServices = listOf(ServiceName.cas2, ServiceName.cas2v2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("f38f55c0-fda6-44f8-a3b1-a7c0a990bc51"),
          name = "hdcNotEligible",
          label = "HDC not eligible",
          applicableToServices = listOf(ServiceName.cas2),
        ),
        Cas2PersistedApplicationStatusDetail(
          id = UUID.fromString("4f1033ab-2dea-47ce-8a86-7c47b3ccadd8"),
          name = "personTransferredToAnotherPrison",
          label = "Person transferred to another prison",
          applicableToServices = listOf(ServiceName.cas2),
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
  fun statusList(service: ServiceName): List<Cas2PersistedApplicationStatus> = statuses
    .mapNotNull { status ->
      when {
        status.statusDetails.isNullOrEmpty() -> status
        else -> {
          val filteredDetails = status.statusDetails
            .filter { it.applicableToServices.contains(service) }
            .takeIf { it.isNotEmpty() }

          filteredDetails?.let { status.copy(statusDetails = it) }
        }
      }
    }
}
