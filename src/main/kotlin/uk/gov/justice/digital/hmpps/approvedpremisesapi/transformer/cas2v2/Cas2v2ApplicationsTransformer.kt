package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component
class Cas2v2ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val cas2v2UserTransformer: Cas2v2UserTransformer,
  private val statusUpdateTransformer: Cas2v2StatusUpdateTransformer,
  private val timelineEventsTransformer: Cas2v2TimelineEventsTransformer,
  private val cas2v2AssessmentsTransformer: Cas2v2AssessmentsTransformer,
) {

  fun transformJpaToApi(jpa: Cas2v2ApplicationEntity, personInfo: PersonInfoResult): Cas2v2Application = Cas2v2Application(
    id = jpa.id,
    person = personTransformer.transformModelToPersonApi(personInfo),
    createdBy = cas2v2UserTransformer.transformJpaToApi(jpa.createdByUser),
    schemaVersion = jpa.schemaVersion.id,
    outdatedSchema = !jpa.schemaUpToDate,
    createdAt = jpa.createdAt.toInstant(),
    submittedAt = jpa.submittedAt?.toInstant(),
    data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
    document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
    status = getStatus(jpa),
    type = "CAS2V2",
    telephoneNumber = jpa.telephoneNumber,
    assessment = if (jpa.assessment != null) cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!) else null,
    timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
    applicationOrigin = jpa.applicationOrigin,
  )

  fun transformJpaSummaryToSummary(
    jpaSummary: Cas2v2ApplicationSummaryEntity,
    personName: String,
  ): Cas2v2ApplicationSummary = Cas2v2ApplicationSummary(
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
    applicationOrigin = when (jpaSummary.applicationOrigin) {
      "courtBail" -> ApplicationOrigin.courtBail
      "prisonBail" -> ApplicationOrigin.prisonBail
      else -> ApplicationOrigin.homeDetentionCurfew
    },
  )

  private fun getStatus(entity: Cas2v2ApplicationEntity): ApplicationStatus {
    if (entity.submittedAt !== null) {
      return ApplicationStatus.submitted
    }

    return ApplicationStatus.inProgress
  }

  private fun getStatusFromSummary(summary: Cas2v2ApplicationSummaryEntity): ApplicationStatus = when {
    summary.submittedAt != null -> ApplicationStatus.submitted
    else -> ApplicationStatus.inProgress
  }
}
