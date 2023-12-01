package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
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
      sex = probationOffenderResult.probationOffenderDetail.gender ?: "Not found",
      status = inOutStatusToPersonInfoApiStatus(probationOffenderResult.inmateDetail?.inOutStatus),
      nomsNumber = probationOffenderResult.probationOffenderDetail.otherIds.nomsNumber,
      pncNumber = probationOffenderResult.probationOffenderDetail.otherIds.pncNumber ?: "Not found",
      nationality = probationOffenderResult.probationOffenderDetail.offenderProfile?.nationality ?: "Not found",
      prisonName = inOutStatusToPersonInfoApiStatus(probationOffenderResult.inmateDetail?.inOutStatus).takeIf { it == FullPerson.Status.inCustody }?.let {
        probationOffenderResult.inmateDetail?.assignedLivingUnit?.agencyName
          ?: probationOffenderResult.inmateDetail?.assignedLivingUnit?.agencyId
      },
    )

  fun transformCas2ApplicationEntityToPersonApi(application: Cas2ApplicationEntity) =
    FullPerson(
      type = PersonType.fullPerson,
      crn = application.crn,
      name = application.name,
      dateOfBirth = application.dateOfBirth,
      sex = application.sex ?: "Not found",
      status = FullPerson.Status.valueOf(application.personStatus),
      nomsNumber = application.nomsNumber,
      pncNumber = application.pncNumber ?: "Not found",
      nationality = application.nationality ?: "Not found",
      prisonName = application.prisonName,
    )

  fun transformCas2ApplicationSummaryToPersonApi(applicationSummary: Cas2ApplicationSummary) =
    FullPerson(
      type = PersonType.fullPerson,
      crn = applicationSummary.getCrn(),
      name = applicationSummary.getName(),
      dateOfBirth = applicationSummary.getDateOfBirth(),
      sex = applicationSummary.getSex() ?: "Not found",
      status = FullPerson.Status.valueOf(applicationSummary.getPersonStatus()),
      nomsNumber = applicationSummary.getNomsNumber(),
      pncNumber = applicationSummary.getPncNumber() ?: "Not found",
      nationality = applicationSummary.getNationality() ?: "Not found",
      prisonName = applicationSummary.getPrisonName(),
    )

  private fun inOutStatusToPersonInfoApiStatus(inOutStatus: InOutStatus?) = when (inOutStatus) {
    InOutStatus.IN -> FullPerson.Status.inCustody
    InOutStatus.OUT -> FullPerson.Status.inCommunity
    InOutStatus.TRN -> FullPerson.Status.inCustody
    null -> FullPerson.Status.unknown
  }
}
