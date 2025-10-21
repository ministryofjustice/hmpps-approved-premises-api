package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity

@Component
class Cas2v2AssessmentsTransformer(
  private val statusUpdateTransformer: Cas2v2StatusUpdateTransformer,
) {
  fun transformJpaToApiRepresentation(
    jpaAssessment: Cas2AssessmentEntity,
  ): Cas2v2Assessment = Cas2v2Assessment(
    jpaAssessment.id,
    jpaAssessment.nacroReferralId,
    jpaAssessment.assessorName,
    jpaAssessment.statusUpdates?.map { update -> statusUpdateTransformer.transformJpaToApi(update) },
  )
}
