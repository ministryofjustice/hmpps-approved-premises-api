package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class Cas1ChangeRequestReasonEntityFactory : Factory<Cas1ChangeRequestReasonEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var code: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var changeRequestType: Yielded<ChangeRequestType> = { ChangeRequestType.APPEAL }
  private var archived: Yielded<Boolean> = { false }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withChangeRequestType(changeRequestType: ChangeRequestType) = apply {
    this.changeRequestType = { changeRequestType }
  }

  fun withArchived(archived: Boolean) = apply {
    this.archived = { archived }
  }

  override fun produce() = Cas1ChangeRequestReasonEntity(
    this.id(),
    this.code(),
    this.changeRequestType(),
    this.archived(),
  )
}
