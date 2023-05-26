package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import java.util.UUID

class UserRoleAssignmentEntityFactory : Factory<UserRoleAssignmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var user: Yielded<UserEntity>? = null
  private var role: Yielded<UserRole> = { UserRole.APPLICANT }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withUser(user: UserEntity) = apply {
    this.user = { user }
  }

  fun withRole(role: UserRole) = apply {
    this.role = { role }
  }

  override fun produce(): UserRoleAssignmentEntity = UserRoleAssignmentEntity(
    id = this.id(),
    user = this.user?.invoke() ?: throw RuntimeException("Must provide a User"),
    role = this.role(),
  )
}
