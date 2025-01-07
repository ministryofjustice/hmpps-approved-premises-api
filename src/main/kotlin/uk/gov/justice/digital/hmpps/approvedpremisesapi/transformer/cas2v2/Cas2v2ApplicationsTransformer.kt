package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component("Cas2v2ApplicationsTransformer")
class Cas2v2ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
  private val statusUpdateTransformer: Cas2v2StatusUpdateTransformer,
  private val timelineEventsTransformer: Cas2v2TimelineEventsTransformer,
  private val assessmentsTransformer: Cas2v2AssessmentsTransformer,
) {

  fun transformJpaToApi(jpa: Cas2v2ApplicationEntity, personInfo: PersonInfoResult): Cas2Application {
    return Cas2Application(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdBy = nomisUserTransformer.transformJpaToApi(jpa.createdByUser),
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      status = getStatus(jpa),
      type = "CAS2",
      telephoneNumber = jpa.telephoneNumber,
      assessment = if (jpa.assessment != null) assessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!) else null,
      timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
    )
  }

  fun transformJpaSummaryToSummary(
    jpaSummary: Cas2v2ApplicationSummaryEntity,
    personName: String,
  ): Cas2v2ApplicationSummary {
    return Cas2v2ApplicationSummary(
        id = jpaSummary.id,
        createdByUserId = UUID.fromString(jpaSummary.userId),
        createdByUserName = jpaSummary.userName,
        createdAt = jpaSummary.createdAt.toInstant(),
        submittedAt = jpaSummary.submittedAt?.toInstant(),
        status = getStatusFromSummary(jpaSummary),
        latestStatusUpdate = statusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(jpaSummary),
        type = "CAS2",
        hdcEligibilityDate = jpaSummary.hdcEligibilityDate,
        crn = jpaSummary.crn,
        nomsNumber = jpaSummary.nomsNumber,
        personName = personName,
      )
  }

  private fun getStatus(entity: Cas2v2ApplicationEntity): ApplicationStatus {
    if (entity.submittedAt !== null) {
      return ApplicationStatus.submitted
    }

    return ApplicationStatus.inProgress
  }

  private fun getStatusFromSummary(summary: Cas2v2ApplicationSummaryEntity): ApplicationStatus {
    return when {
      summary.submittedAt != null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }
}
