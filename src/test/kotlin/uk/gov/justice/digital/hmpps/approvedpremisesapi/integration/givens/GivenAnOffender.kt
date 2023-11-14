package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProfileFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.asOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockServerErrorOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockServerErrorInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.time.LocalDate
import java.time.ZonedDateTime

fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForCommunityApi: Boolean = false,
  mockServerErrorForPrisonApi: Boolean = false,
): Pair<CaseDetail, InmateDetail> {
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

  val caseDetail = CaseDetailFactory()
    .withCase(offenderDetails.asCaseSummary())
    .withMappaDetail(
      MappaDetail(2, "M2", 2, "M2", LocalDate.now().minusDays(1), ZonedDateTime.now()),
    )
    .produce()

  APDeliusContext_mockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)
  APDeliusContext_mockSuccessfulCaseSummaryCall(listOf(offenderDetails.otherIds.crn), CaseSummaries(listOf(caseDetail.case)))
  mockOffenderUserAccessCall(offenderDetails.otherIds.crn, false, false)

  when (mockServerErrorForPrisonApi) {
    true -> PrisonAPI_mockServerErrorInmateDetailsCall(inmateDetails.offenderNo)
    false -> PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetails)
  }

  loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

  return Pair(caseDetail, inmateDetails)
}

fun OffenderDetailSummary.asCaseSummary() = CaseSummaryFactory()
  .withCrn(otherIds.crn)
  .withNomsId(otherIds.nomsNumber)
  .withGender(gender)
  .withName(
    NameFactory()
      .withForename(firstName)
      .withSurname(surname)
      .withMiddleNames(
        middleNames?.let {
          middleNames
        } ?: emptyList(),
      )
      .produce(),
  )
  .withDateOfBirth(dateOfBirth)
  .withProfile(
    ProfileFactory()
      .withReligion(offenderProfile.religion)
      .withEthnicity(offenderProfile.ethnicity)
      .withNationality(offenderProfile.nationality)
      .withGenderIdentity(offenderProfile.genderIdentity)
      .produce(),
  )
  .withCurrentExclusion(currentExclusion)
  .withCurrentRestriction(currentRestriction)
  .produce()

fun IntegrationTestBase.`Given an Offender`(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForCommunityApi: Boolean = false,
  mockServerErrorForPrisonApi: Boolean = false,
  block: (offenderDetails: CaseDetail, inmateDetails: InmateDetail) -> Unit,
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

    val offenderDetails = offenderDetailsFactory.produce().asCaseSummary()

    APDeliusContext_mockSuccessfulCaseSummaryCall(listOf(offenderDetails.crn), CaseSummaries(listOf(offenderDetails)))
    mockOffenderUserAccessCall(offenderDetails.crn, false, false)
    PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetails)
    loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

    Pair(offenderDetails.asOffenderDetail(), inmateDetails)
  }

  block(offenderSequence)
}
