package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class InmateDetailFactory : Factory<InmateDetail> {
  private var offenderNo: Yielded<String> = { randomStringUpperCase(8) }
  private var assignedLivingUnit: Yielded<AssignedLivingUnit?> = { null }
  private var status: Yielded<InmateStatus> = { InmateStatus.OUT }

  fun withOffenderNo(offenderNo: String) = apply {
    this.offenderNo = { offenderNo }
  }

  fun withAssignedLivingUnit(assignedLivingUnit: AssignedLivingUnit) = apply {
    this.assignedLivingUnit = { assignedLivingUnit }
  }

  fun withStatus(status: InmateStatus) = apply {
    this.status = { status }
  }

  override fun produce(): InmateDetail = InmateDetail(
    offenderNo = this.offenderNo(),
    assignedLivingUnit = this.assignedLivingUnit(),
    status = this.status(),
  )
}
