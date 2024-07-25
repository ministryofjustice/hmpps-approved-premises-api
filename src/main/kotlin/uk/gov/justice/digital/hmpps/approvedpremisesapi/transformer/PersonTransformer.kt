package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary

@Component
class PersonTransformer {
  fun transformModelToPersonApi(personInfoResult: PersonInfoResult): Person {
    return when (personInfoResult) {
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
  }

  fun transformSummaryToPersonApi(personInfoResult: PersonSummaryInfoResult): Person {
    return when (personInfoResult) {
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
      type = PersonType.fullPerson,
      crn = probationOffenderDetail.otherIds.crn,
      name = "${probationOffenderDetail.firstName} ${probationOffenderDetail.surname}",
      dateOfBirth = probationOffenderDetail.dateOfBirth!!,
      sex = probationOffenderDetail.gender ?: "Not found",
      status = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus),
      nomsNumber = probationOffenderDetail.otherIds.nomsNumber,
      pncNumber = probationOffenderDetail.otherIds.pncNumber ?: "Not found",
      nationality = probationOffenderDetail.offenderProfile?.nationality ?: "Not found",
      prisonName = inmateStatusToPersonInfoApiStatus(inmateDetail?.custodyStatus).takeIf { it == PersonStatus.inCustody }
        ?.let {
          inmateDetail?.assignedLivingUnit?.agencyName
            ?: inmateDetail?.assignedLivingUnit?.agencyId
        },
      isRestricted = (probationOffenderDetail.currentExclusion ?: false || probationOffenderDetail.currentRestriction ?: false),
    )
  }

  fun inmateStatusToPersonInfoApiStatus(status: InmateStatus?) = when (status) {
    InmateStatus.IN -> PersonStatus.inCustody
    InmateStatus.OUT -> PersonStatus.inCommunity
    InmateStatus.TRN -> PersonStatus.inCustody
    null -> PersonStatus.unknown
  }
}
