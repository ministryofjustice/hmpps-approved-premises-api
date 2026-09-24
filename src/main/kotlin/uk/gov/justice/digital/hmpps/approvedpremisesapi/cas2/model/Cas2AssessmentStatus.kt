package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.util.UUID

enum class Cas2AssessmentStatus(
  val id: UUID,
  val label: String,
  val description: String,
  val hasStatusDetails: Boolean = false,
  val isActive: Boolean = true,
  val lowerCaseName: String,
) {
  MORE_INFO_REQUESTED(
    id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
    label = "More information requested",
    description = "The referrer must provide information requested for the application to progress.",
    hasStatusDetails = true,
    lowerCaseName = "moreInfoRequested",
  ),

  AWAITING_DECISION(
    id = UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1"),
    label = "Awaiting decision",
    description = "The CAS-2 team has the information they need and will make a decision.",
    lowerCaseName = "awaitingDecision",
  ),

  ON_WAITING_LIST(
    id = UUID.fromString("a919097d-b324-471c-9834-756f255e87ea"),
    label = "On waiting list",
    description = "The applicant has been added to the waiting list for Short-Term Accommodation (CAS-2).",
    lowerCaseName = "onWaitingList",
  ),

  PLACE_OFFERED(
    id = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"),
    label = "Place offered",
    description = "The applicant has been offered a place for Short-Term Accommodation (CAS-2).",
    lowerCaseName = "placeOffered",
  ),

  OFFER_ACCEPTED(
    id = UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019"),
    label = "Offer accepted",
    description = "The accommodation offered has been accepted.",
    lowerCaseName = "offerAccepted",
  ),

  OFFER_DECLINED(
    id = UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"),
    label = "Offer declined or withdrawn",
    description = "The accommodation offered has been declined or withdrawn.",
    hasStatusDetails = true,
    lowerCaseName = "offerDeclined",
  ),

  WITHDRAWN(
    id = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"),
    label = "Referral withdrawn",
    description = "The referrer withdrew the application.",
    hasStatusDetails = true,
    lowerCaseName = "withdrawn",
  ),

  CANCELLED(
    id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
    label = "Referral cancelled",
    description = "The application has been cancelled.",
    hasStatusDetails = true,
    lowerCaseName = "cancelled",
  ),

  AWAITING_ARRIVAL(
    id = UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565"),
    label = "Awaiting arrival",
    description = "The accommodation is arranged for the agreed dates.",
    lowerCaseName = "awaitingArrival",
  ),
}
