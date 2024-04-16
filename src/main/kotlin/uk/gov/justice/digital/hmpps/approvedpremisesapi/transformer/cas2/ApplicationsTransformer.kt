package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Component("Cas2ApplicationsTransformer")
class ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
  private val statusUpdateTransformer: StatusUpdateTransformer,
  private val timelineEventsTransformer: TimelineEventsTransformer,
) {

  fun transformJpaToApi(jpa: Cas2ApplicationEntity, personInfo: PersonInfoResult):
    Cas2Application {
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
      statusUpdates = jpa.statusUpdates?.map { statusUpdate -> statusUpdateTransformer.transformJpaToApi(statusUpdate) },
      timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
    )
  }

  fun transformJpaSummaryToSummary(
    jpaSummary: Cas2ApplicationSummary,
    personName: String,
  ): uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary {
    return uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model
      .Cas2ApplicationSummary(
        id = jpaSummary.getId(),
        createdByUserId = jpaSummary.getCreatedByUserId(),
        createdAt = jpaSummary.getCreatedAt().toInstant(),
        submittedAt = jpaSummary.getSubmittedAt()?.toInstant(),
        status = getStatusFromSummary(jpaSummary),
        latestStatusUpdate = statusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(jpaSummary),
        type = "CAS2",
        hdcEligibilityDate = jpaSummary.getHdcEligibilityDate(),
        personName = personName,
      )
  }

  private fun getStatus(entity: Cas2ApplicationEntity): ApplicationStatus {
    if (entity.submittedAt !== null) {
      return ApplicationStatus.submitted
    }

    return ApplicationStatus.inProgress
  }

  private fun getStatusFromSummary(summary: Cas2ApplicationSummary): ApplicationStatus {
    return when {
      summary.getSubmittedAt() != null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }
}
