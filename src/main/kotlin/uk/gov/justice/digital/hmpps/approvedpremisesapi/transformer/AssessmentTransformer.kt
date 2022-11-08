package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer
) {
  fun transformJpaToApi(jpa: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = Assessment(
    id = jpa.id,
    application = applicationsTransformer.transformJpaToApi(jpa.application, offenderDetailSummary, inmateDetail),
    schemaVersion = jpa.schemaVersion.id,
    outdatedSchema = jpa.schemaUpToDate,
    createdAt = jpa.createdAt,
    allocatedAt = jpa.allocatedAt,
    data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
    clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
    allocatedToStaffMemberId = jpa.allocatedToUser.id,
    submittedAt = jpa.submittedAt
  )
}
