package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import java.util.UUID

class UserQualificationAssignmentEntityFactory : Factory<UserQualificationAssignmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var user: Yielded<UserEntity>? = null
  private var qualification: Yielded<UserQualification> = { UserQualification.EMERGENCY }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withUser(user: UserEntity) = apply {
    this.user = { user }
  }

  fun withQualification(qualification: UserQualification) = apply {
    this.qualification = { qualification }
  }

  override fun produce(): UserQualificationAssignmentEntity = UserQualificationAssignmentEntity(
    id = this.id(),
    user = this.user?.invoke() ?: throw RuntimeException("Must provide a User"),
    qualification = this.qualification(),
  )
}
