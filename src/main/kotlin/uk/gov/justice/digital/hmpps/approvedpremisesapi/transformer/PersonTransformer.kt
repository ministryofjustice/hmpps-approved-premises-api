package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus

@Component
class PersonTransformer {
  fun transformModelToPersonApi(personInfoResult: PersonInfoResult.Success) = when (personInfoResult) {
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
    )
    is PersonInfoResult.Success.Restricted -> RestrictedPerson(
      type = PersonType.restrictedPerson,
      crn = personInfoResult.crn,
    )
  }

  private fun inOutStatusToPersonInfoApiStatus(inOutStatus: InOutStatus?) = when (inOutStatus) {
    InOutStatus.IN -> FullPerson.Status.inCustody
    InOutStatus.OUT -> FullPerson.Status.inCommunity
    InOutStatus.TRN -> FullPerson.Status.inCustody
    null -> FullPerson.Status.unknown
  }
}
