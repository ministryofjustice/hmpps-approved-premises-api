package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockServerErrorOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonApiMockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonApiMockServerErrorInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonApiMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail

fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForCommunityApi: Boolean = false,
  mockServerErrorForPrisonApi: Boolean = false,
  mockNotFoundErrorForPrisonApi: Boolean = false,
): Pair<OffenderDetailSummary, InmateDetail> {
  val inmateDetailsFactory = InmateDetailFactory()
  if (inmateDetailsConfigBlock != null) {
    inmateDetailsConfigBlock(inmateDetailsFactory)
  }

  val inmateDetails = inmateDetailsFactory.produce()

  val offenderDetailsFactory = OffenderDetailsSummaryFactory()
    .withNomsNumber(inmateDetails.offenderNo)

  if (offenderDetailsConfigBlock != null) {
    offenderDetailsConfigBlock(offenderDetailsFactory)
  }

  val offenderDetails = offenderDetailsFactory.produce()

  when (mockServerErrorForCommunityApi) {
    true -> communityApiMockServerErrorOffenderDetailsCall(offenderDetails.otherIds.crn)
    false -> communityApiMockSuccessfulOffenderDetailsCall(offenderDetails)
  }

  val caseDetail = offenderDetails.asCaseDetail()

  apDeliusContextMockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

  apDeliusContextMockCaseSummary(caseDetail.case)
  apDeliusContextMockUserAccess(
    CaseAccessFactory()
      .withCrn(offenderDetails.otherIds.crn)
      .withUserExcluded(offenderDetails.currentExclusion)
      .withUserRestricted(offenderDetails.currentRestriction)
      .produce(),
  )

  loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)

  when (mockServerErrorForPrisonApi) {
    true -> prisonApiMockServerErrorInmateDetailsCall(inmateDetails.offenderNo)
    false -> prisonApiMockSuccessfulInmateDetailsCall(inmateDetails)
  }

  when (mockNotFoundErrorForPrisonApi) {
    true -> prisonApiMockNotFoundInmateDetailsCall(inmateDetails.offenderNo)
    false -> {}
  }

  loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

  return Pair(offenderDetails, inmateDetails)
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForCommunityApi: Boolean = false,
  mockServerErrorForPrisonApi: Boolean = false,
  mockNotFoundErrorForPrisonApi: Boolean = false,
  block: (offenderDetails: OffenderDetailSummary, inmateDetails: InmateDetail) -> Unit,
) {
  val (offenderDetails, inmateDetails) = `Given an Offender`(
    offenderDetailsConfigBlock,
    inmateDetailsConfigBlock,
    mockServerErrorForCommunityApi,
    mockServerErrorForPrisonApi,
    mockNotFoundErrorForPrisonApi,
  )

  block(offenderDetails, inmateDetails)
}

fun IntegrationTestBase.`Given Some Offenders`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  block: (offenderSequence: Sequence<Pair<OffenderDetailSummary, InmateDetail>>) -> Unit,
) {
  val offenderSequence = generateSequence {
    val inmateDetailsFactory = InmateDetailFactory()
    if (inmateDetailsConfigBlock != null) {
      inmateDetailsConfigBlock(inmateDetailsFactory)
    }

    val inmateDetails = inmateDetailsFactory.produce()

    val offenderDetailsFactory = OffenderDetailsSummaryFactory()
      .withNomsNumber(inmateDetails.offenderNo)

    if (offenderDetailsConfigBlock != null) {
      offenderDetailsConfigBlock(offenderDetailsFactory)
    }

    val offenderDetails = offenderDetailsFactory.produce()
    val caseDetail = offenderDetails.asCaseDetail()

    communityApiMockSuccessfulOffenderDetailsCall(offenderDetails)

    apDeliusContextMockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

    apDeliusContextMockCaseSummary(caseDetail.case)
    apDeliusContextMockUserAccess(
      CaseAccessFactory()
        .withCrn(offenderDetails.otherIds.crn)
        .withUserExcluded(offenderDetails.currentExclusion)
        .withUserRestricted(offenderDetails.currentRestriction)
        .produce(),
    )

    loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)

    prisonApiMockSuccessfulInmateDetailsCall(inmateDetails)

    loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

    Pair(offenderDetails, inmateDetails)
  }

  block(offenderSequence)
}
