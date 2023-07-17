package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonInfoType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus

@Component
class PersonTransformer {
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
