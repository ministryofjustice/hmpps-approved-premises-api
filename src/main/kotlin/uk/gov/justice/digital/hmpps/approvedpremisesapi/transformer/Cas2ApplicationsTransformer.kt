package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks

@Component
class Cas2ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
) {

  fun transformJpaToApi(jpa: Cas2ApplicationEntity, personInfo: PersonInfoResult.Success):
    Cas2Application {

    return Cas2Application(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdByUserId = jpa.createdByNomisUser.id,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      risks = if (jpa.riskRatings != null) {
        risksTransformer.transformDomainToApi(
          jpa.riskRatings!!,
          jpa.crn,
        )
      } else {
        null
      },
      status = getStatus(jpa),
      type = "CAS2",
    )
  }

  fun transformJpaSummaryToCas2Summary(jpaSummary: Cas2ApplicationSummary, personInfo:
  PersonInfoResult.Success): uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary  {

    val riskRatings =
      if (jpaSummary.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(jpaSummary
        .getRiskRatings()!!) else null

    return uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model
      .Cas2ApplicationSummary(
        id = jpaSummary.getId(),
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = jpaSummary.getCreatedByUserId(),
        createdAt = jpaSummary.getCreatedAt().toInstant(),
        submittedAt = jpaSummary.getSubmittedAt()?.toInstant(),
        risks = if (riskRatings != null) risksTransformer.transformDomainToApi
          (riskRatings, jpaSummary.getCrn()) else null,
        status = getStatusFromSummary(jpaSummary),
        type = "CAS2",
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
