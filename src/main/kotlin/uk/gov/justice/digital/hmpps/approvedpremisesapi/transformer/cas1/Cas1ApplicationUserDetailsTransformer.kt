package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity

@Component
class Cas1ApplicationUserDetailsTransformer {
  fun transformJpaToApi(jpa: Cas1ApplicationUserDetailsEntity) = Cas1ApplicationUserDetails(
    name = jpa.name,
    email = jpa.email,
    telephoneNumber = jpa.telephoneNumber,
  )
}
