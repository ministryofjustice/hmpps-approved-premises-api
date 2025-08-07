package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext

data class UserOffenderAccess(
  val userRestricted: Boolean,
  val userExcluded: Boolean,
  val restrictionMessage: String?,
)
