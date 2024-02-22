package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity

@Component
class AppealTransformer(private val userTransformer: UserTransformer) {
  fun transformJpaToApi(jpa: AppealEntity): Appeal = Appeal(
    id = jpa.id,
    appealDate = jpa.appealDate,
    appealDetail = jpa.appealDetail,
    createdAt = jpa.createdAt.toInstant(),
    applicationId = jpa.application.id,
    createdByUser = userTransformer.transformJpaToApi(jpa.createdBy, ServiceName.approvedPremises),
    decision = AppealDecision.entries.first { it.value == jpa.decision },
    decisionDetail = jpa.decisionDetail,
    assessmentId = jpa.assessment.id,
  )
}
