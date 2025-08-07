package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles

data class NomisStaffInformation(
  val generalAccount: NomisGeneralAccount,
)

data class NomisGeneralAccount(
  val username: String,
)
