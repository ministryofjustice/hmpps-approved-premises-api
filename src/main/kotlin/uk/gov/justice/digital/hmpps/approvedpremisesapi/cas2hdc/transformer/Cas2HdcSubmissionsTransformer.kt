package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcSubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcSubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component("Cas2SubmittedApplicationTransformer")
class Cas2HdcSubmissionsTransformer(
  private val jsonMapper: JsonMapper,
  private val personTransformer: PersonTransformer,
  private val cas2HdcNomisUserTransformer: Cas2HdcNomisUserTransformer,
  private val cas2HdcTimelineEventsTransformer: Cas2HdcTimelineEventsTransformer,
  private val cas2HdcAssessmentsTransformer: Cas2HdcAssessmentsTransformer,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  private val cas2HdcUserService: Cas2HdcUserService,
) {

  fun transformJpaToApiRepresentation(
    jpa: Cas2ApplicationEntity,
    personInfo: PersonInfoResult.Success,
  ): Cas2HdcSubmittedApplication {
    val currentUser = jpa.currentPomUserId?.let { cas2HdcUserService.getNomisUserById(jpa.currentPomUserId!!, jpa.serviceOrigin) }
    val omu = jpa.currentPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it) }
    return Cas2HdcSubmittedApplication(
      id = jpa.id,
      person = personTransformer.personInfoResultToPerson(personInfo),
      submittedBy = cas2HdcNomisUserTransformer.transformJpaToApi(jpa),
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      document = if (jpa.document != null) jsonMapper.readTree(jpa.document) else null,
      telephoneNumber = jpa.telephoneNumber,
      timelineEvents = cas2HdcTimelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
      assessment = cas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
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
  ): Cas2HdcSubmittedApplicationSummary = Cas2HdcSubmittedApplicationSummary(
    id = jpaSummary.id,
    personName = personName,
    createdByUserId = UUID.fromString(jpaSummary.userId),
    createdAt = jpaSummary.createdAt.toInstant(),
    submittedAt = jpaSummary.submittedAt?.toInstant(),
    crn = jpaSummary.crn,
    nomsNumber = jpaSummary.nomsNumber,
  )
}
