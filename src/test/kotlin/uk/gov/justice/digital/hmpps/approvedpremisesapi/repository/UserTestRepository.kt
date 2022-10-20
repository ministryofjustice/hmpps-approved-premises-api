package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import java.util.UUID

@Repository
interface UserTestRepository : JpaRepository<UserEntity, UUID>

@Repository
interface UserRoleAssignmentTestRepository : JpaRepository<UserRoleAssignmentEntity, UUID>

@Repository
interface UserQualificationAssignmentTestRepository : JpaRepository<UserQualificationAssignmentEntity, UUID>
