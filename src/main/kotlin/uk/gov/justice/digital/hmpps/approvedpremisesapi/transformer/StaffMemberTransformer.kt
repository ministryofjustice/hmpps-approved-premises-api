package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember as ApiStaffMember

@Component
class StaffMemberTransformer {
  fun transformDomainToApi(domain: StaffMember) = ApiStaffMember(
    code = domain.code,
    keyWorker = domain.keyWorker,
    name = domain.name.deliusName(),
  )
}
