package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class AssessmentTransformer(private val objectMapper: ObjectMapper, private val applicationsTransformer: ApplicationsTransformer) {
  fun transformJpaToApi(jpa: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = Assessment(
    id = jpa.id,
    application = applicationsTransformer.transformJpaToApi(jpa.application, offenderDetailSummary, inmateDetail),
    schemaVersion = jpa.schemaVersion.id,
    outdatedSchema = jpa.schemaUpToDate,
    createdAt = jpa.createdAt,
    allocatedAt = jpa.allocatedAt,
    data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
    clarificationNotes = jpa.clarificationNotes.map {
      ClarificationNote(
        id = it.id,
        createdAt = it.createdAt,
        createdByStaffMemberId = it.createdByUser.id,
        text = it.text
      )
    },
    allocatedToStaffMemberId = jpa.allocatedToUser.id,
    submittedAt = jpa.submittedAt
  )
}
