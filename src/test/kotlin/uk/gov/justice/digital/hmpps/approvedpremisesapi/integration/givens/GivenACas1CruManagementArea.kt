package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity

fun IntegrationTestBase.givenACas1CruManagementArea(
  assessmentAutoAllocationUsername: String? = null,
  assessmentAutoAllocations: MutableMap<AutoAllocationDay, String> = mutableMapOf(),
): Cas1CruManagementAreaEntity = cas1CruManagementAreaEntityFactory.produceAndPersist {
  if (assessmentAutoAllocations.isEmpty() && assessmentAutoAllocationUsername != null) {
    withAssessmentAutoAllocations(
      mutableMapOf(
        AutoAllocationDay.MONDAY to assessmentAutoAllocationUsername,
        AutoAllocationDay.TUESDAY to assessmentAutoAllocationUsername,
        AutoAllocationDay.WEDNESDAY to assessmentAutoAllocationUsername,
        AutoAllocationDay.THURSDAY to assessmentAutoAllocationUsername,
        AutoAllocationDay.FRIDAY to assessmentAutoAllocationUsername,
        AutoAllocationDay.SATURDAY to assessmentAutoAllocationUsername,
        AutoAllocationDay.SUNDAY to assessmentAutoAllocationUsername,
      ),
    )
  } else {
    withAssessmentAutoAllocations(assessmentAutoAllocations)
  }
}
