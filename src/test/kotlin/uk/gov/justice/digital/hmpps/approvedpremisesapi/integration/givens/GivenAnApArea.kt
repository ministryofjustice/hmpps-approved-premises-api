package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import java.util.UUID

fun IntegrationTestBase.givenAnApArea(
  id: UUID = UUID.randomUUID(),
  name: String? = null,
  defaultCruManagementArea: Cas1CruManagementAreaEntity? = null,
): ApAreaEntity = apAreaEntityFactory.produceAndPersist {
  withId(id)
  if (name != null) {
    withName(name)
  }
  withDefaultCruManagementArea(defaultCruManagementArea ?: givenACas1CruManagementArea())
}
