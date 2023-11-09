package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Component("Cas2SubmittedApplicationTransformer")
class SubmittedApplicationTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
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
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      status = getStatus(jpa),
    )
  }

  fun transformJpaSummaryToCas2SubmittedSummary(
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
      status = getStatusFromSummary(jpaSummary),
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
