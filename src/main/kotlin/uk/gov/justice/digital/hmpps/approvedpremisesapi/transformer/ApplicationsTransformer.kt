package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity

@Component
class ApplicationsTransformer(private val objectMapper: ObjectMapper) {
  fun transformJpaToApi(jpa: ApplicationEntity) = Application(
    id = jpa.id,
    crn = jpa.crn,
    createdByProbationOfficerId = jpa.createdByProbationOfficer.id,
    schemaVersion = jpa.schemaVersion.id,
    outdatedSchema = !jpa.schemaUpToDate,
    createdAt = jpa.createdAt,
    submittedAt = jpa.submittedAt,
    data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null
  )
}
