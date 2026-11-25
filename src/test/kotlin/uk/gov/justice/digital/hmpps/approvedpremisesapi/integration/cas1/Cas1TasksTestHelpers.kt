package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult

fun getOffenderSummaries(offenderDetails: OffenderDetailSummary): List<PersonSummaryInfoResult> = listOf(
  PersonSummaryInfoResult.Success.Full(
    offenderDetails.otherIds.crn,
    CaseSummaryFactory().withName(
      NameFactory()
        .withForename(offenderDetails.firstName)
        .withSurname(offenderDetails.surname)
        .produce(),
    )
      .withDateOfBirth(offenderDetails.dateOfBirth)
      .produce(),
    tier = null,
  ),
)
