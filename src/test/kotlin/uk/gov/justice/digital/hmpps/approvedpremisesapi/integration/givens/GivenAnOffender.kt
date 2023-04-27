package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars

fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  block: (offenderDetails: OffenderDetailSummary, inmateDetails: InmateDetail) -> Unit
) {
  val nomsNumber = randomNumberChars(5)
  val offenderDetailsFactory = OffenderDetailsSummaryFactory()
    .withNomsNumber(nomsNumber)
  val inmateDetailsFactory = InmateDetailFactory()
    .withOffenderNo(nomsNumber)

  if (offenderDetailsConfigBlock != null) {
    offenderDetailsConfigBlock(offenderDetailsFactory)
  }

  if (inmateDetailsConfigBlock != null) {
    inmateDetailsConfigBlock(inmateDetailsFactory)
  }

  val offenderDetails = offenderDetailsFactory.produce()
  val inmateDetails = inmateDetailsFactory.produce()

  CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
  loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)
  PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetails)

  block(offenderDetails, inmateDetails)
}
