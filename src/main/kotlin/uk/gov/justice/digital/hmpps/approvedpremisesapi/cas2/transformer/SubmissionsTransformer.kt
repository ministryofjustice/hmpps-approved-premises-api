package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Component("Cas2SubmittedApplicationTransformer")
class SubmissionsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
  private val timelineEventsTransformer: TimelineEventsTransformer,
  private val assessmentsTransformer: AssessmentsTransformer,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  private val cas2UserService: Cas2UserService,
) {

  fun transformJpaToApiRepresentation(
    jpa: Cas2ApplicationEntity,
    personInfo: PersonInfoResult.Success,
  ): Cas2SubmittedApplication {
    val currentUser = jpa.currentPomUserId?.let { cas2UserService.getCas2UserById(jpa.currentPomUserId!!) }
    val omu = jpa.currentPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it) }
    return Cas2SubmittedApplication(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      submittedBy = nomisUserTransformer.transformJpaToApi(jpa),
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      telephoneNumber = jpa.telephoneNumber,
      timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
      assessment = assessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
      allocatedPomName = currentUser?.name,
      allocatedPomEmailAddress = currentUser?.email,
      currentPrisonName = omu?.prisonName ?: jpa.currentPrisonCode,
      isTransferredApplication = jpa.isTransferredApplication(),
      assignmentDate = jpa.currentAssignmentDate,
      omuEmailAddress = omu?.email,
    )
  }

  fun transformJpaSummaryToApiRepresentation(
    jpaSummary: Cas2ApplicationSummaryEntity,
    personName: String,
  ): Cas2SubmittedApplicationSummary = Cas2SubmittedApplicationSummary(
    id = jpaSummary.id,
    personName = personName,
    createdByUserId = jpaSummary.userId,
    createdAt = jpaSummary.createdAt.toInstant(),
    submittedAt = jpaSummary.submittedAt?.toInstant(),
    crn = jpaSummary.crn,
    nomsNumber = jpaSummary.nomsNumber,
  )
}
