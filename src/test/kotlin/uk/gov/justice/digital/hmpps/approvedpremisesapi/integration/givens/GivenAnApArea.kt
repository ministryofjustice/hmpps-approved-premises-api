package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import java.util.UUID

fun IntegrationTestBase.`Given an AP Area`(
  id: UUID = UUID.randomUUID(),
  name: String? = null,
  emailAddress: String? = null,
): ApAreaEntity {
  return apAreaEntityFactory.produceAndPersist {
    withId(id)
    if (name != null) {
      withName(name)
    }
    if (emailAddress != null) {
      withEmailAddress(emailAddress)
    }
  }
}
