package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

const val SAS_ROLE = "ROLE_ACCOMMODATION_API__SINGLE_ACCOMMODATION_SERVICE"

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenASingleAccommodationServiceClientCredentialsApiCall(
  block: (clientCredentialsJwt: String) -> Unit,
) {
  val clientCredentialsJwt = jwtAuthHelper.createValidClientCredentialsJwt(
    role = SAS_ROLE,
  )
  return block(clientCredentialsJwt)
}
