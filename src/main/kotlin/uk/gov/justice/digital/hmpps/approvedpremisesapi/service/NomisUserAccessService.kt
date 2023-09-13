package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity

class NomisUserAccessService {
  fun userCanViewApplication(user: NomisUserEntity, cas2Application:
  Cas2ApplicationEntity) : Boolean {
    return user.id == cas2Application.createdByNomisUser.id
  }
}