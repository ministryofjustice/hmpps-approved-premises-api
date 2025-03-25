package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisGeneralAccount
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisStaffInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class NomisStaffInformationFactory : Factory<NomisStaffInformation> {
  private var generalAccount: Yielded<NomisGeneralAccount> = { NomisGeneralAccountFactory().produce() }

  fun withNomisGeneralAccount(generalAccount: NomisGeneralAccount) = apply {
    this.generalAccount = { generalAccount }
  }

  override fun produce(): NomisStaffInformation = NomisStaffInformation(
    generalAccount = this.generalAccount(),
  )
}

class NomisGeneralAccountFactory : Factory<NomisGeneralAccount> {
  private var username: Yielded<String> = { randomStringUpperCase(8) }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): NomisGeneralAccount = NomisGeneralAccount(
    username = this.username(),
  )
}
