package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult

@Component
class PersonTransformer {

  fun personInfoResultToPersonSummaryInfoResult(
    personInfo: PersonInfoResult,
  ): PersonSummaryInfoResult = when (personInfo) {
    is PersonInfoResult.NotFound -> PersonSummaryInfoResult.NotFound(
      crn = personInfo.crn,
    )

    is PersonInfoResult.Success.Full -> PersonSummaryInfoResult.Success.Full(
      crn = personInfo.crn,
      summary = personInfo.offenderDetailSummary.asCaseSummary(),
    )

    is PersonInfoResult.Success.Restricted -> PersonSummaryInfoResult.Success.Restricted(
      crn = personInfo.crn,
      nomsNumber = personInfo.nomsNumber,
    )

    is PersonInfoResult.Unknown -> PersonSummaryInfoResult.Unknown(
      crn = personInfo.crn,
    )
  }

  fun personSummaryInfoToPersonSummary(
    personSummaryInfo: PersonSummaryInfoResult,
  ): PersonSummary {
    when (personSummaryInfo) {
      is PersonSummaryInfoResult.Success.Full -> {
        return FullPersonSummary(
          crn = personSummaryInfo.crn,
          personType = PersonSummaryDiscriminator.fullPersonSummary,
          name = getNameFromPersonSummaryInfoResult(personSummaryInfo),
          isRestricted = personSummaryInfo.summary.currentRestriction || personSummaryInfo.summary.currentExclusion,
        )
      }

      is PersonSummaryInfoResult.Success.Restricted -> {
        return RestrictedPersonSummary(
          crn = personSummaryInfo.crn,
          personType = PersonSummaryDiscriminator.restrictedPersonSummary,
        )
      }

      is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> {
        return UnknownPersonSummary(
          crn = personSummaryInfo.crn,
          personType = PersonSummaryDiscriminator.unknownPersonSummary,
        )
      }
    }
  }

  fun transformModelToPersonApi(personInfoResult: PersonInfoResult): Person = when (personInfoResult) {
    is PersonInfoResult.Success.Full -> {
      val offenderDetailSummary = personInfoResult.offenderDetailSummary
      val inmateDetail = personInfoResult.inmateDetail
      FullPerson(
        type = PersonType.fullPerson,
        crn = offenderDetailSummary.otherIds.crn,
        pncNumber = offenderDetailSummary.otherIds.pncNumber,
        name = "${offenderDetailSummary.firstName} ${offenderDetailSummary.surname}",
        dateOfBirth = offenderDetailSummary.dateOfBirth,
        sex = offenderDetailSummary.gender,
        status = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus),
        nomsNumber = inmateDetail?.offenderNo,
        ethnicity = offenderDetailSummary.offenderProfile.ethnicity,
        nationality = offenderDetailSummary.offenderProfile.nationality,
        religionOrBelief = offenderDetailSummary.offenderProfile.religion,
        genderIdentity = when (offenderDetailSummary.offenderProfile.genderIdentity) {
          "Prefer to self-describe" -> offenderDetailSummary.offenderProfile.selfDescribedGender
          else -> offenderDetailSummary.offenderProfile.genderIdentity
        },
        prisonName = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus).takeIf { it == PersonStatus.inCustody }
          ?.let {
            inmateDetail?.assignedLivingUnit?.agencyName
              ?: inmateDetail?.assignedLivingUnit?.agencyId
          },
        // this is used by Cas2 which ignores currentExclusion, isRestricted flag is not used on the cas 2 frontend so it can be left in
        isRestricted = (offenderDetailSummary.currentExclusion || offenderDetailSummary.currentRestriction),
      )
    }

    is PersonInfoResult.Success.Restricted -> RestrictedPerson(
      type = PersonType.restrictedPerson,
      crn = personInfoResult.crn,
    )

    is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> UnknownPerson(
      type = PersonType.unknownPerson,
      crn = personInfoResult.crn,
    )
  }

  fun transformSummaryToPersonApi(personInfoResult: PersonSummaryInfoResult): Person = when (personInfoResult) {
    is PersonSummaryInfoResult.Success.Full -> {
      val caseSummary = personInfoResult.summary
      FullPerson(
        type = PersonType.fullPerson,
        crn = personInfoResult.crn,
        name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
        dateOfBirth = caseSummary.dateOfBirth,
        sex = caseSummary.gender ?: "Not Found",
        status = PersonStatus.unknown,
        nomsNumber = caseSummary.nomsId,
        ethnicity = caseSummary.profile?.ethnicity,
        nationality = caseSummary.profile?.nationality,
        religionOrBelief = caseSummary.profile?.religion,
        genderIdentity = caseSummary.profile?.genderIdentity,
        prisonName = null,
        isRestricted = (caseSummary.currentRestriction == true || caseSummary.currentExclusion == true),
      )
    }

    is PersonSummaryInfoResult.Success.Restricted -> RestrictedPerson(
      type = PersonType.restrictedPerson,
      crn = personInfoResult.crn,
    )

    is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> UnknownPerson(
      type = PersonType.unknownPerson,
      crn = personInfoResult.crn,
    )
  }

  fun transformPersonSummaryInfoToPersonInfo(personSummaryInfoResult: PersonSummaryInfoResult, inmateStatus: InmateDetail?): PersonInfoResult = when (personSummaryInfoResult) {
    is PersonSummaryInfoResult.Success.Full -> PersonInfoResult.Success.Full(personSummaryInfoResult.crn, personSummaryInfoResult.summary.asOffenderDetailSummary(), inmateStatus)
    is PersonSummaryInfoResult.Success.Restricted -> PersonInfoResult.Success.Restricted(personSummaryInfoResult.crn, personSummaryInfoResult.nomsNumber)
    is PersonSummaryInfoResult.NotFound -> PersonInfoResult.NotFound(personSummaryInfoResult.crn)
    is PersonSummaryInfoResult.Unknown -> PersonInfoResult.Unknown(personSummaryInfoResult.crn, personSummaryInfoResult.throwable)
  }

  fun transformPersonInfoResultToPersonSummary(personInfo: PersonInfoResult): PersonSummary = when (personInfo) {
    is PersonInfoResult.Success.Full -> FullPersonSummary(personInfo.offenderDetailSummary.surname, false, personInfo.crn, PersonSummaryDiscriminator.fullPersonSummary)
    is PersonInfoResult.Success.Restricted -> RestrictedPersonSummary(personInfo.crn, PersonSummaryDiscriminator.restrictedPersonSummary)
    is PersonInfoResult.NotFound,
    is PersonInfoResult.Unknown,
    -> UnknownPersonSummary(personInfo.crn, PersonSummaryDiscriminator.unknownPersonSummary)
  }

  fun transformProbationOffenderToPersonApi(probationOffenderResult: ProbationOffenderSearchResult.Success.Full): FullPerson {
    val caseSummary = probationOffenderResult.caseSummary
    val inmateDetail = probationOffenderResult.inmateDetail
    return FullPerson(
      type = PersonType.fullPerson,
      crn = caseSummary.crn,
      name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
      dateOfBirth = caseSummary.dateOfBirth,
      sex = caseSummary.gender ?: "Not found",
      status = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus),
      nomsNumber = caseSummary.nomsId,
      pncNumber = caseSummary.pnc ?: "Not found",
      nationality = caseSummary.profile?.nationality ?: "Not found",
      prisonName = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus).takeIf { it == PersonStatus.inCustody }
        ?.let {
          inmateDetail?.assignedLivingUnit?.agencyName
            ?: inmateDetail?.assignedLivingUnit?.agencyId
        },
      isRestricted = caseSummary.currentExclusion || caseSummary.currentRestriction,
    )
  }

  fun inmateStatusToPersonInfoApiStatus(status: InmateStatus?) = when (status) {
    InmateStatus.IN -> PersonStatus.inCustody
    InmateStatus.OUT -> PersonStatus.inCommunity
    InmateStatus.TRN -> PersonStatus.inCustody
    null -> PersonStatus.unknown
  }
}
