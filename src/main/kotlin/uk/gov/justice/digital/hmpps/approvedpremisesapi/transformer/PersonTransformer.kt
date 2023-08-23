package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
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
}
