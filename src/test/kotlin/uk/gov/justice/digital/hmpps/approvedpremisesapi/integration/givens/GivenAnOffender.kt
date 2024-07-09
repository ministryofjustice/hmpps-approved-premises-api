package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_mockCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_mockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockServerErrorOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockServerErrorInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulInmateDetailsCall
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
    true -> CommunityAPI_mockServerErrorOffenderDetailsCall(offenderDetails.otherIds.crn)
    false -> CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
  }

  val caseDetail = offenderDetails.asCaseDetail()

  APDeliusContext_mockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

  ApDeliusContext_mockCaseSummary(caseDetail.case)
  ApDeliusContext_mockUserAccess(
    CaseAccessFactory()
      .withCrn(offenderDetails.otherIds.crn)
      .withUserExcluded(offenderDetails.currentExclusion)
      .withUserRestricted(offenderDetails.currentRestriction)
      .produce(),
  )

  loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)

  when (mockServerErrorForPrisonApi) {
    true -> PrisonAPI_mockServerErrorInmateDetailsCall(inmateDetails.offenderNo)
    false -> PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetails)
  }

  when (mockNotFoundErrorForPrisonApi) {
    true -> PrisonAPI_mockNotFoundInmateDetailsCall(inmateDetails.offenderNo)
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
  block: ((offenderDetails: OffenderDetailSummary, inmateDetails: InmateDetail) -> Unit)? = null,
): Pair<OffenderDetailSummary, InmateDetail> {
  val (offenderDetails, inmateDetails) = `Given an Offender`(
    offenderDetailsConfigBlock,
    inmateDetailsConfigBlock,
    mockServerErrorForCommunityApi,
    mockServerErrorForPrisonApi,
    mockNotFoundErrorForPrisonApi,
  )

  block?.invoke(offenderDetails, inmateDetails)

  return Pair(offenderDetails, inmateDetails)
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

    CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)

    APDeliusContext_mockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

    ApDeliusContext_mockCaseSummary(caseDetail.case)
    ApDeliusContext_mockUserAccess(
      CaseAccessFactory()
        .withCrn(offenderDetails.otherIds.crn)
        .withUserExcluded(offenderDetails.currentExclusion)
        .withUserRestricted(offenderDetails.currentRestriction)
        .produce(),
    )

    loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)

    PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetails)

    loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

    Pair(offenderDetails, inmateDetails)
  }

  block(offenderSequence)
}
