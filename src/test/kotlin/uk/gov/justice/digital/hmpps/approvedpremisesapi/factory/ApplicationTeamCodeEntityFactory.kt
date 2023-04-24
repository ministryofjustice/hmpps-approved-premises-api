package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class ApplicationTeamCodeEntityFactory : Factory<ApplicationTeamCodeEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var application: Yielded<ApprovedPremisesApplicationEntity>? = null
  private var teamCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }
  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }
  fun withTeamCode(teamCode: String) = apply {
    this.teamCode = { teamCode }
  }

  override fun produce() = ApplicationTeamCodeEntity(
    id = this.id(),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an Application"),
    teamCode = this.teamCode()
  )
}
