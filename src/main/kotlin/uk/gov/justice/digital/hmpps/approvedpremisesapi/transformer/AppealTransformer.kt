package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity

@Component
class AppealTransformer {
  fun transformJpaToApi(jpa: AppealEntity): Appeal = Appeal(
    id = jpa.id,
    appealDate = jpa.appealDate,
    appealDetail = jpa.appealDetail,
    reviewer = jpa.reviewer,
    createdAt = jpa.createdAt.toInstant(),
    applicationId = jpa.application.id,
    createdByUserId = jpa.createdBy.id,
    decision = AppealDecision.entries.first { it.value == jpa.decision },
    decisionDetail = jpa.decisionDetail,
    assessmentId = jpa.assessment?.id,
  )
}
