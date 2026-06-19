package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateNonAssignable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component
class Cas2ApplicationsTransformer(
  private val jsonMapper: JsonMapper,
  private val personTransformer: PersonTransformer,
  private val cas2UserTransformer: Cas2UserTransformer,
  private val statusUpdateTransformer: Cas2StatusUpdateTransformer,
  private val timelineEventsTransformer: Cas2TimelineEventsTransformer,
  private val cas2AssessmentsTransformer: Cas2AssessmentsTransformer,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
) {

  fun transformJpaToApi(jpa: Cas2ApplicationEntity, personInfo: PersonInfoResult): Cas2Application = transformJpaAndFullPersonToApi(jpa, personTransformer.transformModelToPersonApi(personInfo))

  fun transformJpaAndFullPersonToApi(jpa: Cas2ApplicationEntity, fullPerson: Person) = Cas2Application(
    id = jpa.id,
    person = fullPerson,
    createdBy = cas2UserTransformer.transformJpaToApi(jpa.createdByUser),
    createdAt = jpa.createdAt.toInstant(),
    submittedAt = jpa.submittedAt?.toInstant(),
    data = if (jpa.data != null) jsonMapper.readTree(jpa.data) else null,
    document = if (jpa.document != null) jsonMapper.readTree(jpa.document) else null,
    status = getStatus(jpa),
    type = "CAS2V2",
    telephoneNumber = jpa.telephoneNumber,
    assessment = if (jpa.assessment != null) cas2AssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!) else null,
    timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
    applicationOrigin = jpa.applicationOrigin,
    cohort = jpa.cohort?.apiType,
  )

  fun transformJpaAndFullPersonToApiSubmitted(jpa: Cas2ApplicationEntity, fullPerson: Person): Cas2SubmittedApplication = Cas2SubmittedApplication(
    id = jpa.id,
    person = fullPerson,
    submittedBy = Cas2UserTransformer().transformJpaToApi(jpa.createdByUser),
    createdAt = jpa.createdAt.toInstant(),
    submittedAt = jpa.submittedAt?.toInstant(),
    document = if (jpa.document != null) jsonMapper.readTree(jpa.document) else null,
    telephoneNumber = jpa.telephoneNumber,
    assessment = cas2AssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
    timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
    applicationOrigin = jpa.applicationOrigin,
  )

  fun transformJpaSummaryToSummary(
    jpaSummary: Cas2ApplicationSummaryEntity,
    personName: String,
  ): Cas2ApplicationSummary = Cas2ApplicationSummary(
    id = jpaSummary.id,
    createdByUserId = UUID.fromString(jpaSummary.userId),
    createdByUserName = jpaSummary.userName,
    createdAt = jpaSummary.createdAt.toInstant(),
    submittedAt = jpaSummary.submittedAt?.toInstant(),
    status = getStatusFromSummary(jpaSummary),
    latestStatusUpdate = statusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(jpaSummary),
    type = "CAS2V2",
    hdcEligibilityDate = jpaSummary.hdcEligibilityDate,
    crn = jpaSummary.crn,
    nomsNumber = jpaSummary.nomsNumber,
    personName = personName,
    prisonCode = jpaSummary.prisonCode,
    applicationOrigin = jpaSummary.applicationOrigin?.let { ApplicationOrigin.forValue(it) } ?: ApplicationOrigin.homeDetentionCurfew,
    cohort = jpaSummary.cohort?.apiType,
  )

  fun applicationOriginFromText(applicationOrigin: String): ApplicationOrigin = when (applicationOrigin) {
    "courtBail" -> ApplicationOrigin.courtBail
    "prisonBail" -> ApplicationOrigin.prisonBail
    "other" -> throw NotImplementedError("Support for 'other' application origin is not yet implemented")
    else -> ApplicationOrigin.homeDetentionCurfew
  }

  fun transformJpaToCas2ReferralHistory(
    jpa: Cas2ApplicationEntity,
  ): Cas2ReferralHistory {
    val latestStatusUpdate = jpa.statusUpdates?.firstOrNull()
    val rejectionReason = latestStatusUpdate
      ?.takeIf { it.label in listOf(Cas2StatusUpdateNonAssignable.REFERRAL_CANCELLED.label, Cas2StatusUpdateNonAssignable.REFERRAL_WITHDRAWN.label) }
      ?.label

    val omu = jpa.referringPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it) }
    val placementAddress = omu?.prisonName ?: jpa.referringPrisonCode ?: throw IllegalStateException("Missing placement address for CAS2v2 application ${jpa.id}")

    return Cas2ReferralHistory(
      id = jpa.assessment!!.id,
      applicationId = jpa.id,
      type = ServiceType.CAS2v2,
      createdAt = jpa.submittedAt!!.toInstant(),
      status = jpa.statusUpdates!!.first().label,
      referralRejectionReason = rejectionReason,
      localAuthorityArea = placementAddress,
      pdu = jpa.preferredAreas,
      referredBy = jpa.createdByUser.name,
      placementAddress = placementAddress,
      placementStatus = latestStatusUpdate?.label,
    )
  }

  private fun getStatus(entity: Cas2ApplicationEntity): ApplicationStatus {
    if (entity.submittedAt !== null) {
      return ApplicationStatus.submitted
    }

    return ApplicationStatus.inProgress
  }

  private fun getStatusFromSummary(summary: Cas2ApplicationSummaryEntity): ApplicationStatus = when {
    summary.submittedAt != null -> ApplicationStatus.submitted
    else -> ApplicationStatus.inProgress
  }
}
