package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProfileFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockServerErrorOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockServerErrorInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForCommunityApi: Boolean = false,
  mockServerErrorForPrisonApi: Boolean = false,
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

  val caseDetail = CaseDetailFactory().withCase(
    CaseSummaryFactory()
      .withCrn(offenderDetails.otherIds.crn)
      .withNomsId(offenderDetails.otherIds.nomsNumber)
      .withGender(offenderDetails.gender)
      .withName(
        NameFactory()
          .withForename(offenderDetails.firstName)
          .withSurname(offenderDetails.surname)
          .withMiddleNames(
            offenderDetails.middleNames?.let {
              offenderDetails.middleNames
            } ?: emptyList(),
          )
          .produce(),
      )
      .withDateOfBirth(offenderDetails.dateOfBirth)
      .withProfile(
        ProfileFactory()
          .withReligion(offenderDetails.offenderProfile.religion)
          .withEthnicity(offenderDetails.offenderProfile.ethnicity)
          .withNationality(offenderDetails.offenderProfile.nationality)
          .withGenderIdentity(offenderDetails.offenderProfile.genderIdentity)
          .produce(),
      )
      .withCurrentExclusion(offenderDetails.currentExclusion)
      .withCurrentRestriction(offenderDetails.currentRestriction)
      .produce(),
  ).produce()

  APDeliusContext_mockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

  ApDeliusContext_addCaseSummaryToBulkResponse(
    caseDetail.case,
  )
  ApDeliusContext_addResponseToUserAccessCall(
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

  loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

  return Pair(offenderDetails, inmateDetails)
}

fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForCommunityApi: Boolean = false,
  mockServerErrorForPrisonApi: Boolean = false,
  block: (offenderDetails: OffenderDetailSummary, inmateDetails: InmateDetail) -> Unit,
) {
  val (offenderDetails, inmateDetails) = `Given an Offender`(
    offenderDetailsConfigBlock,
    inmateDetailsConfigBlock,
    mockServerErrorForCommunityApi,
    mockServerErrorForPrisonApi,
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

    CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
    loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)
    PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetails)
    loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

    Pair(offenderDetails, inmateDetails)
  }

  block(offenderSequence)
}
