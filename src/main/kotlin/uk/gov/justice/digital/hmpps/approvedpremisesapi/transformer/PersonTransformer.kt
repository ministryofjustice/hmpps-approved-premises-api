package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonInfoType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class PersonTransformer {
  fun transformModelToApi(offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail?) = Person(
    crn = offenderDetailSummary.otherIds.crn,
    name = "${offenderDetailSummary.firstName} ${offenderDetailSummary.surname}",
    dateOfBirth = offenderDetailSummary.dateOfBirth,
    sex = offenderDetailSummary.gender,
    status = inOutStatusToApiStatus(inmateDetail?.inOutStatus),
    nomsNumber = inmateDetail?.offenderNo,
    ethnicity = offenderDetailSummary.offenderProfile.ethnicity,
    nationality = offenderDetailSummary.offenderProfile.nationality,
    religionOrBelief = offenderDetailSummary.offenderProfile.religion,
    genderIdentity = when (offenderDetailSummary.offenderProfile.genderIdentity) {
      "Prefer to self-describe" -> offenderDetailSummary.offenderProfile.selfDescribedGender
      else -> offenderDetailSummary.offenderProfile.genderIdentity
    },
    prisonName = inOutStatusToApiStatus(inmateDetail?.inOutStatus).takeIf { it == Person.Status.inCustody }?.let {
      inmateDetail?.assignedLivingUnit?.agencyName ?: inmateDetail?.assignedLivingUnit?.agencyId
    },
  )

  private fun inOutStatusToApiStatus(inOutStatus: InOutStatus?) = when (inOutStatus) {
    InOutStatus.IN -> Person.Status.inCustody
    InOutStatus.OUT -> Person.Status.inCommunity
    InOutStatus.TRN -> Person.Status.inCustody
    null -> Person.Status.unknown
  }

  fun transformModelToPersonInfoApi(personInfoResult: PersonInfoResult.Success) = when (personInfoResult) {
    is PersonInfoResult.Success.Full -> FullPersonInfo(
      type = PersonInfoType.fullPersonInfo,
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
      prisonName = inOutStatusToPersonInfoApiStatus(personInfoResult.inmateDetail?.inOutStatus).takeIf { it == FullPersonInfo.Status.inCustody }?.let {
        personInfoResult.inmateDetail?.assignedLivingUnit?.agencyName ?: personInfoResult.inmateDetail?.assignedLivingUnit?.agencyId
      },
    )
    is PersonInfoResult.Success.Restricted -> RestrictedPersonInfo(
      type = PersonInfoType.restrictedPersonInfo,
      crn = personInfoResult.crn,
    )
  }

  private fun inOutStatusToPersonInfoApiStatus(inOutStatus: InOutStatus?) = when (inOutStatus) {
    InOutStatus.IN -> FullPersonInfo.Status.inCustody
    InOutStatus.OUT -> FullPersonInfo.Status.inCommunity
    InOutStatus.TRN -> FullPersonInfo.Status.inCustody
    null -> FullPersonInfo.Status.unknown
  }
}
