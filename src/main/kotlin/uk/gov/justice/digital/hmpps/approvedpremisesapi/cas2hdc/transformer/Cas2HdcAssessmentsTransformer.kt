package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentEntity

@Component("Cas2AssessmentsTransformer")
class Cas2HdcAssessmentsTransformer(private val cas2HdcStatusUpdateTransformer: Cas2HdcStatusUpdateTransformer) {
  fun transformJpaToApiRepresentation(
    jpaAssessment: Cas2AssessmentEntity,
  ): Cas2Assessment = Cas2Assessment(
    jpaAssessment.id,
    jpaAssessment.nacroReferralId,
    jpaAssessment.assessorName,
    jpaAssessment.statusUpdates?.map { update -> cas2HdcStatusUpdateTransformer.transformJpaToApi(update) },
  )
}
