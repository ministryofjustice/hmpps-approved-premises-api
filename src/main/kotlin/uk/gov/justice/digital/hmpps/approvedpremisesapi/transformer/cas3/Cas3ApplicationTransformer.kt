package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer

@Component
class Cas3ApplicationTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
) {
  fun transformJpaToApi(applicationEntity: TemporaryAccommodationApplicationEntity, personInfo: PersonInfoResult): Cas3Application {
    val latestAssessment = applicationEntity.getLatestAssessment()

    return Cas3Application(
      id = applicationEntity.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdByUserId = applicationEntity.createdByUser.id,
      schemaVersion = applicationEntity.schemaVersion.id,
      outdatedSchema = !applicationEntity.schemaUpToDate,
      createdAt = applicationEntity.createdAt.toInstant(),
      submittedAt = applicationEntity.submittedAt?.toInstant(),
      arrivalDate = applicationEntity.arrivalDate?.toInstant(),
      data = if (applicationEntity.data != null) objectMapper.readTree(applicationEntity.data) else null,
      document = if (applicationEntity.document != null) objectMapper.readTree(applicationEntity.document) else null,
      risks = if (applicationEntity.riskRatings != null) {
        risksTransformer.transformDomainToApi(
          applicationEntity.riskRatings!!,
          applicationEntity.crn,
        )
      } else {
        null
      },
      status = getStatus(applicationEntity, latestAssessment),
      offenceId = applicationEntity.offenceId,
      assessmentId = latestAssessment?.id,
    )
  }

  fun transformDomainToCas3ApplicationSummary(
    domain: ApplicationSummary,
    personInfo: PersonInfoResult,
  ): Cas3ApplicationSummary {
    val riskRatings =
      if (domain.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

    return Cas3ApplicationSummary(
      id = domain.getId(),
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdByUserId = domain.getCreatedByUserId(),
      createdAt = domain.getCreatedAt(),
      submittedAt = domain.getSubmittedAt(),
      risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
      status = getStatusFromSummary(domain as TemporaryAccommodationApplicationSummary),
    )
  }

  private fun getStatusFromSummary(entity: TemporaryAccommodationApplicationSummary): ApplicationStatus = when {
    entity.getLatestAssessmentHasClarificationNotesWithoutResponse() -> ApplicationStatus.requestedFurtherInformation
    entity.getSubmittedAt() !== null -> ApplicationStatus.submitted
    else -> ApplicationStatus.inProgress
  }

  private fun getStatus(entity: ApplicationEntity, latestAssessment: AssessmentEntity?): ApplicationStatus = when {
    latestAssessment?.clarificationNotes?.any { it.response == null } == true -> ApplicationStatus.requestedFurtherInformation
    entity.submittedAt !== null -> ApplicationStatus.submitted
    else -> ApplicationStatus.inProgress
  }
}
