package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember

@Component
class StaffMemberTransformer() {
  fun transformDomainToApi(domain: uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffMember) = StaffMember(id = domain.staffIdentifier, name = "${domain.staff.forenames} ${domain.staff.surname}")
}
