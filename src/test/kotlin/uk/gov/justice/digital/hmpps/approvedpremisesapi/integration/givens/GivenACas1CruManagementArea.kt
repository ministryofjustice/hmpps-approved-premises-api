package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity

fun IntegrationTestBase.`Given an CAS1 CRU Management Area`(): Cas1CruManagementAreaEntity {
  return cas1CruManagementAreaEntityFactory.produceAndPersist()
}
