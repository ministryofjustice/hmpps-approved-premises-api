package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

fun UserEntity.addRoleForUnitTest(role: UserRole) = apply {
  this.roles += UserRoleAssignmentEntityFactory()
    .withRole(role)
    .withUser(this)
    .produce()
}

fun UserEntity.addQualificationForUnitTest(qualification: UserQualification) = apply {
  this.qualifications += UserQualificationAssignmentEntityFactory()
    .withQualification(qualification)
    .withUser(this)
    .produce()
}
