package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Person

@Component
class PersonTransformer() {
  fun transformModelToApi(model: uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Person) = Person(crn = model.crn, name = model.name)
}
