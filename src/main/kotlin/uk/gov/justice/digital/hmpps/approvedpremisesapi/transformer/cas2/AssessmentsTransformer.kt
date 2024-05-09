package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity

@Component("Cas2AssessmentsTransformer")
class AssessmentsTransformer(private val statusUpdateTransformer: StatusUpdateTransformer) {
  fun transformJpaToApiRepresentation(
    jpaAssessment: Cas2AssessmentEntity,
  ): Cas2Assessment {
    return Cas2Assessment(
      jpaAssessment.id,
      jpaAssessment.nacroReferralId,
      jpaAssessment.assessorName,
      jpaAssessment.statusUpdates?.map { update -> statusUpdateTransformer.transformJpaToApi(update) },
    )
  }
}
