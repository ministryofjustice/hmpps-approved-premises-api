package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary

@Component
class PersonTransformer() {
  fun transformModelToApi(model: OffenderDetailSummary) = Person(crn = model.otherIds.crn, name = "${model.firstName} ${model.surname}")
}
