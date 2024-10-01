package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import java.util.UUID

fun IntegrationTestBase.`Given an AP Area`(
  id: UUID = UUID.randomUUID(),
  name: String? = null,
  defaultCruManagementArea: Cas1CruManagementAreaEntity? = null,
): ApAreaEntity {
  return apAreaEntityFactory.produceAndPersist {
    withId(id)
    if (name != null) {
      withName(name)
    }
    withDefaultCruManagementArea(defaultCruManagementArea ?: `Given a CAS1 CRU Management Area`())
  }
}
