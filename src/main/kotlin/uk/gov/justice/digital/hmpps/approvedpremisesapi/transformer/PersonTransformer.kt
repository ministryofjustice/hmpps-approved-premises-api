package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PersonEntity

@Component
class PersonTransformer() {
  fun transformJpaToApi(jpa: PersonEntity) = Person(crn = jpa.crn, name = jpa.name)
}
