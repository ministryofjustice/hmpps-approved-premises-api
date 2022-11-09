package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ContextStaffMember

@Component
class StaffMemberTransformer() {
  fun transformDomainToApi(domain: ContextStaffMember) = StaffMember(
    code = domain.code,
    keyWorker = domain.keyWorker,
    name = "${domain.name.forename} ${domain.name.surname}"
  )
}
