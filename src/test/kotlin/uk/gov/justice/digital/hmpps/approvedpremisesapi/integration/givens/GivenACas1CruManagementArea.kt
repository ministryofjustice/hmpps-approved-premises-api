package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity

fun IntegrationTestBase.givenACas1CruManagementArea(
  assessmentAutoAllocationUsername: String? = null,
  assessmentAutoAllocations: MutableMap<AutoAllocationDay, String> = mutableMapOf(),
): Cas1CruManagementAreaEntity = cas1CruManagementAreaEntityFactory.produceAndPersist {
  withAssessmentAutoAllocationUsername(assessmentAutoAllocationUsername)
  withAssessmentAutoAllocations(assessmentAutoAllocations)
}
