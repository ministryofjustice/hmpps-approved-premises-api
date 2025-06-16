package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity

@Component("Cas2AssessmentsTransformer")
class AssessmentsTransformer(private val statusUpdateTransformer: StatusUpdateTransformer) {
  fun transformJpaToApiRepresentation(
    jpaAssessment: Cas2AssessmentEntity,
  ): Cas2Assessment = Cas2Assessment(
    jpaAssessment.id,
    jpaAssessment.nacroReferralId,
    jpaAssessment.assessorName,
    jpaAssessment.statusUpdates?.map { update -> statusUpdateTransformer.transformJpaToApi(update) },
  )
}
