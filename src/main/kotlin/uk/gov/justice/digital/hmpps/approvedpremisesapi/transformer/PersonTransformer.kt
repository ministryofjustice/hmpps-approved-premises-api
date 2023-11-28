package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus

@Component
class PersonTransformer {
  fun transformModelToPersonApi(personInfoResult: PersonInfoResult) = when (personInfoResult) {
    is PersonInfoResult.Success.Full -> FullPerson(
      type = PersonType.fullPerson,
      crn = personInfoResult.offenderDetailSummary.otherIds.crn,
      name = "${personInfoResult.offenderDetailSummary.firstName} ${personInfoResult.offenderDetailSummary.surname}",
      dateOfBirth = personInfoResult.offenderDetailSummary.dateOfBirth,
      sex = personInfoResult.offenderDetailSummary.gender,
      status = inOutStatusToPersonInfoApiStatus(personInfoResult.inmateDetail?.inOutStatus),
      nomsNumber = personInfoResult.inmateDetail?.offenderNo,
      ethnicity = personInfoResult.offenderDetailSummary.offenderProfile.ethnicity,
      nationality = personInfoResult.offenderDetailSummary.offenderProfile.nationality,
      religionOrBelief = personInfoResult.offenderDetailSummary.offenderProfile.religion,
      genderIdentity = when (personInfoResult.offenderDetailSummary.offenderProfile.genderIdentity) {
        "Prefer to self-describe" -> personInfoResult.offenderDetailSummary.offenderProfile.selfDescribedGender
        else -> personInfoResult.offenderDetailSummary.offenderProfile.genderIdentity
      },
      prisonName = inOutStatusToPersonInfoApiStatus(personInfoResult.inmateDetail?.inOutStatus).takeIf { it == FullPerson.Status.inCustody }?.let {
        personInfoResult.inmateDetail?.assignedLivingUnit?.agencyName ?: personInfoResult.inmateDetail?.assignedLivingUnit?.agencyId
      },
      isRestricted = (personInfoResult.offenderDetailSummary.currentExclusion || personInfoResult.offenderDetailSummary.currentRestriction),
    )
    is PersonInfoResult.Success.Restricted -> RestrictedPerson(
      type = PersonType.restrictedPerson,
      crn = personInfoResult.crn,
    )
    is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> UnknownPerson(
      type = PersonType.unknownPerson,
      crn = personInfoResult.crn,
    )
  }

  fun transformSummaryToPersonApi(personInfoResult: PersonSummaryInfoResult) = when (personInfoResult) {
    is PersonSummaryInfoResult.Success.Full -> FullPerson(
      type = PersonType.fullPerson,
      crn = personInfoResult.crn,
      name = "${personInfoResult.summary.name.forename} ${personInfoResult.summary.name.surname}",
      dateOfBirth = personInfoResult.summary.dateOfBirth,
      sex = personInfoResult.summary.gender ?: "Not Found",
      status = FullPerson.Status.unknown,
      nomsNumber = personInfoResult.summary.nomsId,
      ethnicity = personInfoResult.summary.profile?.ethnicity,
      nationality = personInfoResult.summary.profile?.nationality,
      religionOrBelief = personInfoResult.summary.profile?.religion,
      genderIdentity = personInfoResult.summary.profile?.genderIdentity,
      prisonName = null,
      isRestricted = (personInfoResult.summary.currentRestriction == true || personInfoResult.summary.currentExclusion == true),
    )
    is PersonSummaryInfoResult.Success.Restricted -> RestrictedPerson(
      type = PersonType.restrictedPerson,
      crn = personInfoResult.crn,
    )
    is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> UnknownPerson(
      type = PersonType.unknownPerson,
      crn = personInfoResult.crn,
    )
  }

  fun transformProbationOffenderToPersonApi(probationOffenderResult: ProbationOffenderSearchResult.Success.Full, nomsNumber: String) =
    FullPerson(
      type = PersonType.fullPerson,
      crn = probationOffenderResult.probationOffenderDetail.otherIds.crn,
      name = "${probationOffenderResult.probationOffenderDetail.firstName} ${probationOffenderResult.probationOffenderDetail.surname}",
      dateOfBirth = probationOffenderResult.probationOffenderDetail.dateOfBirth!!,
      sex = probationOffenderResult.probationOffenderDetail.gender ?: "Not Found",
      status = inOutStatusToPersonInfoApiStatus(probationOffenderResult.inmateDetail?.inOutStatus),
      nomsNumber = probationOffenderResult.probationOffenderDetail.otherIds.nomsNumber,
      nationality = probationOffenderResult.probationOffenderDetail.offenderProfile?.nationality
        ?: "Not Found",
      pncNumber = probationOffenderResult.probationOffenderDetail.otherIds.pncNumber ?: "Not found",
      prisonName = inOutStatusToPersonInfoApiStatus(probationOffenderResult.inmateDetail?.inOutStatus).takeIf { it == FullPerson.Status.inCustody }?.let {
        probationOffenderResult.inmateDetail?.assignedLivingUnit?.agencyName
          ?: probationOffenderResult.inmateDetail?.assignedLivingUnit?.agencyId
      },
      isRestricted = (probationOffenderResult.probationOffenderDetail.currentExclusion ?: false || probationOffenderResult.probationOffenderDetail.currentRestriction ?: false),
    )
  private fun inOutStatusToPersonInfoApiStatus(inOutStatus: InOutStatus?) = when (inOutStatus) {
    InOutStatus.IN -> FullPerson.Status.inCustody
    InOutStatus.OUT -> FullPerson.Status.inCommunity
    InOutStatus.TRN -> FullPerson.Status.inCustody
    null -> FullPerson.Status.unknown
  }
}
