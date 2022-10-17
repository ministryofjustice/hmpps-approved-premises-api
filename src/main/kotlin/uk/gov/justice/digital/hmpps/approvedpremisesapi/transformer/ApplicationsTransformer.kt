package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class ApplicationsTransformer(private val objectMapper: ObjectMapper, private val personTransformer: PersonTransformer) {
  fun transformJpaToApi(jpa: ApplicationEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = Application(
    id = jpa.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    createdByProbationOfficerId = jpa.createdByProbationOfficer.id,
    schemaVersion = jpa.schemaVersion.id,
    outdatedSchema = !jpa.schemaUpToDate,
    createdAt = jpa.createdAt,
    submittedAt = jpa.submittedAt,
    isWomensApplication = jpa.isWomensApplication,
    isPipeApplication = jpa.isPipeApplication,
    data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
    document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null
  )
}
