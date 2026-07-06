package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component
class Cas2SubmissionsTransformer(
  private val jsonMapper: JsonMapper,
  private val personTransformer: PersonTransformer,
  private val cas2UserTransformer: Cas2UserTransformer,
  private val cas2TimelineEventsTransformer: Cas2TimelineEventsTransformer,
  private val cas2AssessmentsTransformer: Cas2AssessmentsTransformer,
) {

  fun transformJpaToApiRepresentation(
    jpa: Cas2ApplicationEntity,
    personInfo: PersonInfoResult
      .Success,
  ): Cas2SubmittedApplication = Cas2SubmittedApplication(
    id = jpa.id,
    person = personTransformer.personInfoResultToPerson(personInfo),
    submittedBy = cas2UserTransformer.transformJpaToApi(jpa.createdByUser),
    createdAt = jpa.createdAt.toInstant(),
    submittedAt = jpa.submittedAt?.toInstant(),
    document = if (jpa.document != null) jsonMapper.readTree(jpa.document) else null,
    telephoneNumber = jpa.telephoneNumber,
    applicationOrigin = jpa.applicationOrigin,
    timelineEvents = cas2TimelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
    assessment = cas2AssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
  )

  fun transformJpaSummaryToApiRepresentation(
    jpaSummary: Cas2ApplicationSummaryEntity,
    personName: String,
  ): Cas2SubmittedApplicationSummary = Cas2SubmittedApplicationSummary(
    id = jpaSummary.id,
    personName = personName,
    createdByUserId = UUID.fromString(jpaSummary.userId),
    createdAt = jpaSummary.createdAt.toInstant(),
    submittedAt = jpaSummary.submittedAt?.toInstant(),
    crn = jpaSummary.crn,
    nomsNumber = jpaSummary.nomsNumber,
  )
}
