package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2Assessment

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
