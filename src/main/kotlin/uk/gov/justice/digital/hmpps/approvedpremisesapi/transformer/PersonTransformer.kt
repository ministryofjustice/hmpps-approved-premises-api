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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult

@Component
class PersonTransformer {

  fun personSummaryInfoToPersonSummary(
    personSummaryInfo: PersonSummaryInfoResult,
  ): PersonSummary {
    when (personSummaryInfo) {
      is PersonSummaryInfoResult.Success.Full -> {
        return FullPersonSummary(
          crn = personSummaryInfo.crn,
          personType = PersonSummaryDiscriminator.FULL_PERSON_SUMMARY,
          name = getNameFromPersonSummaryInfoResult(personSummaryInfo),
        )
      }
      is PersonSummaryInfoResult.Success.Restricted -> {
        return RestrictedPersonSummary(
          crn = personSummaryInfo.crn,
          personType = PersonSummaryDiscriminator.RESTRICTED_PERSON_SUMMARY,
        )
      }
      is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> {
        return UnknownPersonSummary(
          crn = personSummaryInfo.crn,
          personType = PersonSummaryDiscriminator.UNKNOWN_PERSON_SUMMARY,
        )
      }
    }
  }

  fun transformModelToPersonApi(personInfoResult: PersonInfoResult): Person {
    return when (personInfoResult) {
      is PersonInfoResult.Success.Full -> {
        val offenderDetailSummary = personInfoResult.offenderDetailSummary
        val inmateDetail = personInfoResult.inmateDetail
        FullPerson(
          type = PersonType.FULL_PERSON,
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
          prisonName = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus).takeIf { it == PersonStatus.IN_CUSTODY }
            ?.let {
              inmateDetail?.assignedLivingUnit?.agencyName
                ?: inmateDetail?.assignedLivingUnit?.agencyId
            },
          isRestricted = (offenderDetailSummary.currentExclusion || offenderDetailSummary.currentRestriction),
        )
      }

      is PersonInfoResult.Success.Restricted -> RestrictedPerson(
        type = PersonType.RESTRICTED_PERSON,
        crn = personInfoResult.crn,
      )

      is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> UnknownPerson(
        type = PersonType.UNKNOWN_PERSON,
        crn = personInfoResult.crn,
      )
    }
  }

  fun transformSummaryToPersonApi(personInfoResult: PersonSummaryInfoResult): Person {
    return when (personInfoResult) {
      is PersonSummaryInfoResult.Success.Full -> {
        val caseSummary = personInfoResult.summary
        FullPerson(
          type = PersonType.FULL_PERSON,
          crn = personInfoResult.crn,
          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
          dateOfBirth = caseSummary.dateOfBirth,
          sex = caseSummary.gender ?: "Not Found",
          status = PersonStatus.UNKNOWN,
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
        type = PersonType.RESTRICTED_PERSON,
        crn = personInfoResult.crn,
      )

      is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> UnknownPerson(
        type = PersonType.UNKNOWN_PERSON,
        crn = personInfoResult.crn,
      )
    }
  }

  fun transformPersonSummaryInfoToPersonInfo(personSummaryInfoResult: PersonSummaryInfoResult, inmateStatus: InmateDetail?): PersonInfoResult {
    return when (personSummaryInfoResult) {
      is PersonSummaryInfoResult.Success.Full -> PersonInfoResult.Success.Full(personSummaryInfoResult.crn, personSummaryInfoResult.summary.asOffenderDetailSummary(), inmateStatus)
      is PersonSummaryInfoResult.Success.Restricted -> PersonInfoResult.Success.Restricted(personSummaryInfoResult.crn, personSummaryInfoResult.nomsNumber)
      is PersonSummaryInfoResult.NotFound -> PersonInfoResult.NotFound(personSummaryInfoResult.crn)
      is PersonSummaryInfoResult.Unknown -> PersonInfoResult.Unknown(personSummaryInfoResult.crn, personSummaryInfoResult.throwable)
    }
  }
  fun transformProbationOffenderToPersonApi(probationOffenderResult: ProbationOffenderSearchResult.Success.Full, nomsNumber: String): FullPerson {
    val probationOffenderDetail = probationOffenderResult.probationOffenderDetail
    val inmateDetail = probationOffenderResult.inmateDetail
    return FullPerson(
      type = PersonType.FULL_PERSON,
      crn = probationOffenderDetail.otherIds.crn,
      name = "${probationOffenderDetail.firstName} ${probationOffenderDetail.surname}",
      dateOfBirth = probationOffenderDetail.dateOfBirth!!,
      sex = probationOffenderDetail.gender ?: "Not found",
      status = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus),
      nomsNumber = probationOffenderDetail.otherIds.nomsNumber,
      pncNumber = probationOffenderDetail.otherIds.pncNumber ?: "Not found",
      nationality = probationOffenderDetail.offenderProfile?.nationality ?: "Not found",
      prisonName = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus).takeIf { it == PersonStatus.IN_CUSTODY }
        ?.let {
          inmateDetail?.assignedLivingUnit?.agencyName
            ?: inmateDetail?.assignedLivingUnit?.agencyId
        },
      isRestricted = (probationOffenderDetail.currentExclusion ?: false || probationOffenderDetail.currentRestriction ?: false),
    )
  }

  fun inmateStatusToPersonInfoApiStatus(status: InmateStatus?) = when (status) {
    InmateStatus.IN -> PersonStatus.IN_CUSTODY
    InmateStatus.OUT -> PersonStatus.IN_COMMUNITY
    InmateStatus.TRN -> PersonStatus.IN_CUSTODY
    null -> PersonStatus.IN_CUSTODY
  }
}
