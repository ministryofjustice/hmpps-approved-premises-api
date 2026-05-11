package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.givenASarClientCredentialsApiCall(
  role: String = "SAR_DATA_ACCESS",
  block: (clientCredentialsJwt: String) -> Unit,
) {
  val clientCredentialsJwt = jwtAuthHelper.createValidClientCredentialsJwt(
    role = if (role.startsWith("ROLE_")) role else "ROLE_$role",
  )
  return block(clientCredentialsJwt)
}
