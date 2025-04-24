package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity

object DomainEventUtils {
  fun mapApprovedPremisesEntityToPremises(aPEntity: ApprovedPremisesEntity) = Premises(
    id = aPEntity.id,
    name = aPEntity.name,
    apCode = aPEntity.apCode,
    legacyApCode = aPEntity.qCode,
    localAuthorityAreaName = aPEntity.localAuthorityArea!!.name,
  )
}
