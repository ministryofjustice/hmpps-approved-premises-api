package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Component("Cas2SubmittedApplicationTransformer")
class SubmissionsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
  private val statusUpdateTransformer: StatusUpdateTransformer,
  private val timelineEventsTransformer: TimelineEventsTransformer,
  private val assessmentsTransformer: AssessmentsTransformer,
) {

  fun transformJpaToApiRepresentation(
    jpa: Cas2ApplicationEntity,
    personInfo:
      PersonInfoResult
      .Success,
  ):
    Cas2SubmittedApplication {
    return Cas2SubmittedApplication(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      submittedBy = nomisUserTransformer.transformJpaToApi(jpa.createdByUser),
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      statusUpdates = jpa.statusUpdates?.map { update -> transformUpdate(update) },
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      telephoneNumber = jpa.telephoneNumber,
      timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
      assessment = assessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
    )
  }

  fun transformJpaSummaryToApiRepresentation(
    jpaSummary: Cas2ApplicationSummary,
    personInfo:
      PersonInfoResult.Success,
  ): Cas2SubmittedApplicationSummary {
    return Cas2SubmittedApplicationSummary(
      id = jpaSummary.getId(),
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdByUserId = jpaSummary.getCreatedByUserId(),
      createdAt = jpaSummary.getCreatedAt().toInstant(),
      submittedAt = jpaSummary.getSubmittedAt()?.toInstant(),
      crn = jpaSummary.getCrn(),
      nomsNumber = jpaSummary.getNomsNumber(),
    )
  }

  private fun transformUpdate(jpa: Cas2StatusUpdateEntity): Cas2StatusUpdate {
    return statusUpdateTransformer.transformJpaToApi(jpa)
  }
}
