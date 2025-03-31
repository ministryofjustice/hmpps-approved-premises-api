package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles

data class NomisStaffInformation(
  val generalAccount: NomisGeneralAccount,
)

data class NomisGeneralAccount(
  val username: String,
)
