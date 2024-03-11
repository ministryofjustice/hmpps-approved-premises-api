package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus

@Component
class PersonTransformer {
  fun transformModelToPersonApi(personInfoResult: PersonInfoResult) = when (personInfoResult) {
    is PersonInfoResult.Success.Full -> FullPerson(
      type = PersonType.fullPerson,
      crn = personInfoResult.offenderDetailSummary.otherIds.crn,
      pncNumber = personInfoResult.offenderDetailSummary.otherIds.pncNumber,
      name = "${personInfoResult.offenderDetailSummary.firstName} ${personInfoResult.offenderDetailSummary.surname}",
      dateOfBirth = personInfoResult.offenderDetailSummary.dateOfBirth,
      sex = personInfoResult.offenderDetailSummary.gender,
      status = inmateStatusToPersonInfoApiStatus(personInfoResult.inmateDetail?.custodyStatus),
      nomsNumber = personInfoResult.inmateDetail?.offenderNo,
      ethnicity = personInfoResult.offenderDetailSummary.offenderProfile.ethnicity,
      nationality = personInfoResult.offenderDetailSummary.offenderProfile.nationality,
      religionOrBelief = personInfoResult.offenderDetailSummary.offenderProfile.religion,
      genderIdentity = when (personInfoResult.offenderDetailSummary.offenderProfile.genderIdentity) {
        "Prefer to self-describe" -> personInfoResult.offenderDetailSummary.offenderProfile.selfDescribedGender
        else -> personInfoResult.offenderDetailSummary.offenderProfile.genderIdentity
      },
      prisonName = inmateStatusToPersonInfoApiStatus(personInfoResult.inmateDetail?.custodyStatus).takeIf { it == PersonStatus.inCustody }?.let {
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
      status = PersonStatus.unknown,
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
      sex = probationOffenderResult.probationOffenderDetail.gender ?: "Not found",
      status = inmateStatusToPersonInfoApiStatus(probationOffenderResult.inmateDetail?.custodyStatus),
      nomsNumber = probationOffenderResult.probationOffenderDetail.otherIds.nomsNumber,
      pncNumber = probationOffenderResult.probationOffenderDetail.otherIds.pncNumber ?: "Not found",
      nationality = probationOffenderResult.probationOffenderDetail.offenderProfile?.nationality ?: "Not found",
      prisonName = inmateStatusToPersonInfoApiStatus(probationOffenderResult.inmateDetail?.custodyStatus).takeIf { it == PersonStatus.inCustody }?.let {
        probationOffenderResult.inmateDetail?.assignedLivingUnit?.agencyName
          ?: probationOffenderResult.inmateDetail?.assignedLivingUnit?.agencyId
      },
      isRestricted = (probationOffenderResult.probationOffenderDetail.currentExclusion ?: false || probationOffenderResult.probationOffenderDetail.currentRestriction ?: false),
    )

  fun inmateStatusToPersonInfoApiStatus(status: InmateStatus?) = when (status) {
    InmateStatus.IN -> PersonStatus.inCustody
    InmateStatus.OUT -> PersonStatus.inCommunity
    InmateStatus.TRN -> PersonStatus.inCustody
    null -> PersonStatus.unknown
  }
}
