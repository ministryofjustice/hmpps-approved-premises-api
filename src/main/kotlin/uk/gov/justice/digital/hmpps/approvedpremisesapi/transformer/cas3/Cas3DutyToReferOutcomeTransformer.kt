package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3DutyToReferOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferOutcomeEntity

@Component
class Cas3DutyToReferOutcomeTransformer {
  fun transformJpaToApi(jpa: Cas3DutyToReferOutcomeEntity) = Cas3DutyToReferOutcome(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
  )
}
